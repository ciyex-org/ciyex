-- V108: Fix patient chart tab configs (phase 2)
-- 1. Immunizations: revert vaccineCode select → cvxCode coded (CVX system, searches via ciyex-codes)
-- 2. Billing: add referringProvider lookup field (after existing provider field)
-- 3. Denials: change broken lookup on request → text field
-- 4. Claims: fix facilityReference endpoint (api/facilities→api/locations)
-- 5. Payment tab: use Invoice with patientSearchParam so data links to patient
-- 6. Transactions tab: remove PaymentReconciliation (no patient scope) – keep only Claim
-- 7. Credentialing app: add service_url so proxy works
-- NOTE: Uses jsonb_set / DO blocks / regexp_replace instead of text REPLACE
-- to survive PostgreSQL JSONB key-order normalization.

-- ─── 1. Immunizations: restore cvxCode coded field ───
-- V107 tried to change cvxCode→vaccineCode (select) but REPLACE may not have matched
-- due to JSONB key-order normalization. Force-set field[0] to the correct coded definition.
UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config,
    '{sections,0,fields,0}',
    '{"key":"cvxCode","label":"Vaccine (CVX)","type":"coded","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Immunization","path":"vaccineCode.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/cvx"}}'::jsonb
),
    updated_at = now()
WHERE tab_key = 'immunizations'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 2. Billing: add referringProvider after the existing provider field ───
-- Uses a DO block to walk fields array and insert after the 'provider' key.
DO $$
DECLARE
    v_config  jsonb;
    v_fields  jsonb;
    v_new_flds jsonb := '[]'::jsonb;
    v_field   jsonb;
    v_ref_prov jsonb := '{
        "key":"referringProvider",
        "label":"Referring Provider",
        "type":"lookup",
        "required":false,
        "colSpan":1,
        "lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},
        "fhirMapping":{"resource":"Claim","path":"referral.reference","type":"reference"}
    }'::jsonb;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = 'billing' AND practice_type_code = '*' AND org_id = '*';

    IF v_config IS NULL OR v_config::text LIKE '%referringProvider%' THEN RETURN; END IF;

    v_fields := v_config -> 'sections' -> 0 -> 'fields';

    FOR v_field IN SELECT value FROM jsonb_array_elements(v_fields) LOOP
        v_new_flds := v_new_flds || jsonb_build_array(v_field);
        IF v_field ->> 'key' = 'provider' THEN
            v_new_flds := v_new_flds || jsonb_build_array(v_ref_prov);
        END IF;
    END LOOP;

    UPDATE tab_field_config
    SET field_config = jsonb_set(v_config, '{sections,0,fields}', v_new_flds),
        updated_at   = now()
    WHERE tab_key = 'billing' AND practice_type_code = '*' AND org_id = '*';
END $$;

-- ─── 3. Denials: fix "request" field – change from lookup (no config) to text ───
-- The lookup had no lookupConfig endpoint so search always returned 404.
DO $$
DECLARE
    v_config    jsonb;
    v_sec_idx   int;
    v_fld_idx   int;
    v_fields    jsonb;
    v_field     jsonb;
    v_updated   jsonb;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = 'claim-denials' AND practice_type_code = '*' AND org_id = '*';

    IF v_config IS NULL THEN RETURN; END IF;

    FOR v_sec_idx IN 0 .. jsonb_array_length(v_config -> 'sections') - 1 LOOP
        v_fields := v_config -> 'sections' -> v_sec_idx -> 'fields';
        FOR v_fld_idx IN 0 .. jsonb_array_length(v_fields) - 1 LOOP
            v_field := v_fields -> v_fld_idx;
            IF v_field ->> 'key' = 'request' AND v_field ->> 'type' = 'lookup' THEN
                -- Merge override keys then strip lookupConfig
                v_updated := (v_field
                    || '{"type":"text","label":"Original Claim Ref","placeholder":"e.g. Claim/123","fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"string"}}'::jsonb)
                    - 'lookupConfig';
                v_config := jsonb_set(
                    v_config,
                    ARRAY['sections', v_sec_idx::text, 'fields', v_fld_idx::text],
                    v_updated
                );
                UPDATE tab_field_config
                SET field_config = v_config,
                    updated_at   = now()
                WHERE tab_key = 'claim-denials' AND practice_type_code = '*' AND org_id = '*';
                RETURN;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- ─── 4. Claims: fix facilityReference endpoint → /api/locations ───
-- REPLACE on JSONB::text is unreliable (key ordering); regexp_replace on the URL string works.
UPDATE tab_field_config
SET field_config = regexp_replace(
    field_config::text,
    '"endpoint":"/api/facilities"',
    '"endpoint":"/api/locations"',
    'g'
)::jsonb,
    updated_at = now()
WHERE tab_key IN ('claims', 'claim-denials', 'era-remittance', 'billing')
  AND practice_type_code = '*'
  AND org_id = '*'
  AND field_config::text LIKE '%/api/facilities%';

-- ─── 5. Payment tab: change to Invoice with subject patientSearchParam ───
-- PaymentNotice has no patient search param; Invoice.subject links to patient properly.
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Invoice","patientSearchParam":"subject"}]',
    field_config = '{
  "sections": [
    {
      "key": "payment-info",
      "title": "Payment Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"date","label":"Payment Date","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"date","type":"date"}},
        {"key":"amount","label":"Amount ($)","type":"number","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"totalNet.value","type":"decimal"}},
        {"key":"paymentType","label":"Payment Type","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"cash","label":"Cash"},{"value":"check","label":"Check"},{"value":"credit_card","label":"Credit Card"},{"value":"debit_card","label":"Debit Card"},{"value":"insurance","label":"Insurance"},{"value":"eft","label":"EFT/ACH"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Invoice","path":"paymentTerms","type":"string"}},
        {"key":"reference","label":"Reference / Check #","type":"text","required":false,"colSpan":1,"placeholder":"e.g. CHK-1234","fhirMapping":{"resource":"Invoice","path":"identifier[0].value","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"issued","label":"Issued"},{"value":"balanced","label":"Balanced"},{"value":"cancelled","label":"Cancelled"}],"badgeColors":{"draft":"bg-gray-100 text-gray-700","issued":"bg-blue-100 text-blue-700","balanced":"bg-green-100 text-green-700","cancelled":"bg-red-100 text-red-700"},"fhirMapping":{"resource":"Invoice","path":"status","type":"code"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Invoice","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'payment'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 6. Transactions tab: remove PaymentReconciliation (no patient param) ───
-- Keep only Claim with patient search param so list filters by patient.
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]',
    updated_at = now()
WHERE tab_key = 'transactions'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND fhir_resources::text LIKE '%PaymentReconciliation%';

-- ─── 7. Credentialing: add service_url so app-proxy can forward requests ───
UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-credentialing.ciyex-credentialing.svc.cluster.local:8080"}'::jsonb
WHERE app_slug = 'ciyex-credentialing'
  AND (config->>'service_url' IS NULL OR config->>'service_url' = '');

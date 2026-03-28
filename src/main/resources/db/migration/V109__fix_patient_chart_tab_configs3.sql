-- V109: Ensure patient chart tab config fixes from V108 were actually applied.
-- V108 used text REPLACE on JSONB columns; PostgreSQL JSONB normalizes key order
-- so those REPLACEs may have silently matched zero rows.
-- This migration re-applies each fix using robust JSONB-aware operations,
-- guarded by conditions so rows that were already corrected are not touched.

-- ─── 1. Immunizations: ensure cvxCode coded field is at sections[0].fields[0] ───
-- Guard: field[0] is not already cvxCode coded type
UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config,
    '{sections,0,fields,0}',
    '{"key":"cvxCode","label":"Vaccine (CVX)","type":"coded","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Immunization","path":"vaccineCode.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/cvx"}}'::jsonb
),
    updated_at = now()
WHERE tab_key = 'immunizations'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND NOT (
      field_config -> 'sections' -> 0 -> 'fields' -> 0 ->> 'key' = 'cvxCode'
      AND field_config -> 'sections' -> 0 -> 'fields' -> 0 ->> 'type' = 'coded'
  );

-- ─── 2. Billing: add referringProvider after the existing provider field ───
-- Guard: referringProvider not already present
DO $$
DECLARE
    v_config   jsonb;
    v_fields   jsonb;
    v_new_flds jsonb := '[]'::jsonb;
    v_field    jsonb;
    v_ref_prov jsonb := '{"key":"referringProvider","label":"Referring Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"referral.reference","type":"reference"}}'::jsonb;
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
-- Guard: request field still has type "lookup"
DO $$
DECLARE
    v_config   jsonb;
    v_sec_idx  int;
    v_fld_idx  int;
    v_fields   jsonb;
    v_field    jsonb;
    v_updated  jsonb;
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
-- Guard: still has /api/facilities in config
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

-- ─── 5. Payment tab: ensure Invoice with subject patientSearchParam ───
-- Guard: not already using Invoice
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
  AND org_id = '*'
  AND fhir_resources::text NOT LIKE '%Invoice%';

-- ─── 6. Transactions: remove PaymentReconciliation ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]',
    updated_at = now()
WHERE tab_key = 'transactions'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND fhir_resources::text LIKE '%PaymentReconciliation%';

-- ─── 7. Credentialing: add service_url ───
UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-credentialing.ciyex-credentialing.svc.cluster.local:8080"}'::jsonb
WHERE app_slug = 'ciyex-credentialing'
  AND (config->>'service_url' IS NULL OR config->>'service_url' = '');

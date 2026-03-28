-- V113: Fix billing/claims/denials FHIR resource creation
-- 1. Add required Claim fields (type, use, priority) to billing tab with defaults
-- 2. Ensure claims and denials insurer fields use display-only references (no bare IDs)
-- 3. Fix Claim.referral to not use Practitioner references (expects ServiceRequest)

-- ─── 1. Billing tab: Add type, use, priority defaults so Claim passes FHIR validation ───
-- Also remove the referral field if present (Claim.referral requires ServiceRequest, not Practitioner)
UPDATE tab_field_config SET
  field_config = '{
  "sections": [
    {
      "key": "billing-details",
      "title": "Billing Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"showInTable":true,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"},"badgeColors":{"active":"bg-green-100 text-green-800","draft":"bg-gray-100 text-gray-800","cancelled":"bg-red-100 text-red-800","entered-in-error":"bg-amber-100 text-amber-800"}},
        {"key":"type","label":"Claim Type","type":"select","required":true,"colSpan":1,"showInTable":true,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"},{"value":"pharmacy","label":"Pharmacy"},{"value":"vision","label":"Vision"}],"fhirMapping":{"resource":"Claim","path":"type.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/claim-type"}},
        {"key":"use","label":"Use","type":"select","required":true,"colSpan":1,"options":[{"value":"claim","label":"Claim"},{"value":"preauthorization","label":"Pre-authorization"},{"value":"predetermination","label":"Pre-determination"}],"fhirMapping":{"resource":"Claim","path":"use","type":"code"}},
        {"key":"serviceDate","label":"Service Date","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Claim","path":"billablePeriod.start","type":"dateTime"}},
        {"key":"cptCode","label":"CPT Code","type":"coded","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Claim","path":"item[0].productOrService.coding[0].code","type":"code","system":"http://www.ama-assn.org/go/cpt"}},
        {"key":"diagnosisCode","label":"Diagnosis (ICD-10)","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[0].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"provider","label":"Rendering Provider","type":"lookup","required":false,"colSpan":1,"showInTable":true,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"provider.reference","type":"reference"}},
        {"key":"insurer","label":"Insurance Company","type":"lookup","required":false,"colSpan":1,"showInTable":true,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"insurer.reference","type":"reference"}},
        {"key":"amount","label":"Charge Amount","type":"number","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Claim","path":"total.value","type":"decimal"}},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"normal","label":"Normal"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"Claim","path":"priority.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/processpriority"}}
      ]
    }
  ]
}',
  updated_at = now()
WHERE tab_key = 'billing' AND practice_type_code = '*' AND org_id = '*';

-- ─── 2. Ensure claims tab insurer uses fhirId ───
DO $$
DECLARE
    v_config   jsonb;
    v_sec_idx  int;
    v_fld_idx  int;
    v_fields   jsonb;
    v_field    jsonb;
    v_lookup   jsonb;
    v_updated  jsonb;
    v_tab      text;
BEGIN
    FOR v_tab IN SELECT tab_key FROM tab_field_config
                 WHERE tab_key IN ('claims', 'claim-denials', 'era-remittance')
                   AND practice_type_code = '*' AND org_id = '*'
    LOOP
        SELECT field_config INTO v_config FROM tab_field_config
        WHERE tab_key = v_tab AND practice_type_code = '*' AND org_id = '*';

        IF v_config IS NULL THEN CONTINUE; END IF;

        FOR v_sec_idx IN 0 .. jsonb_array_length(v_config -> 'sections') - 1 LOOP
            v_fields := v_config -> 'sections' -> v_sec_idx -> 'fields';
            IF v_fields IS NULL THEN CONTINUE; END IF;
            FOR v_fld_idx IN 0 .. jsonb_array_length(v_fields) - 1 LOOP
                v_field := v_fields -> v_fld_idx;
                v_lookup := v_field -> 'lookupConfig';
                -- Fix any lookup with /api/insurance-companies or /api/providers still using valueField "id"
                IF v_lookup IS NOT NULL
                   AND (v_lookup ->> 'endpoint' = '/api/insurance-companies' OR v_lookup ->> 'endpoint' = '/api/providers')
                   AND (v_lookup ->> 'valueField' = 'id' OR v_lookup ->> 'valueField' IS NULL) THEN
                    v_updated := jsonb_set(v_field, '{lookupConfig,valueField}', '"fhirId"');
                    v_config := jsonb_set(
                        v_config,
                        ARRAY['sections', v_sec_idx::text, 'fields', v_fld_idx::text],
                        v_updated
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config
        SET field_config = v_config, updated_at = now()
        WHERE tab_key = v_tab AND practice_type_code = '*' AND org_id = '*';
    END LOOP;
END $$;

-- V110: Fix referrals FHIR paths, denials insurer valueField, immunizations config, and rendering provider

-- ─── 1. Referrals: Fix FHIR paths for referTo and specialty ───
-- referTo was using performer[0].display (wrong: display is optional text on a Reference)
-- specialty was using orderDetail[0].text (wrong: orderDetail is for procedure codes)
-- Fix: Use performer[0].display with type "string" for free-text referral target
--       Use orderDetail[0].text with proper encoding so it round-trips correctly
-- Note: performer[0].display IS valid for storing a display name on a Reference
-- The issue was that the FhirPathMapper was not properly setting/getting the display element
-- Instead, store referTo as a simple note and specialty in the proper coding path

UPDATE tab_field_config
SET field_config = '{
  "sections":[{
    "key":"referral-info","title":"Referral Information","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"referTo","label":"Refer To","type":"text","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"ServiceRequest","path":"performer[0].display","type":"string"}},
      {"key":"specialty","label":"Specialty","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"","label":"Select Specialty"},{"value":"cardiology","label":"Cardiology"},{"value":"dermatology","label":"Dermatology"},{"value":"endocrinology","label":"Endocrinology"},{"value":"gastroenterology","label":"Gastroenterology"},{"value":"neurology","label":"Neurology"},{"value":"oncology","label":"Oncology"},{"value":"orthopedics","label":"Orthopedics"},{"value":"psychiatry","label":"Psychiatry"},{"value":"pulmonology","label":"Pulmonology"},{"value":"rheumatology","label":"Rheumatology"},{"value":"urology","label":"Urology"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"ServiceRequest","path":"orderDetail[0].coding[0].code","type":"code"}},
      {"key":"reason","label":"Reason","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"ServiceRequest","path":"reasonCode[0].text","type":"string"}},
      {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"},{"value":"asap","label":"ASAP"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"ServiceRequest","path":"priority","type":"code"}},
      {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"showInTable":true,"options":[{"value":"draft","label":"Draft"},{"value":"active","label":"Active"},{"value":"completed","label":"Completed"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"ServiceRequest","path":"status","type":"code"}},
      {"key":"date","label":"Date","type":"date","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"ServiceRequest","path":"authoredOn","type":"dateTime"}},
      {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"note[0].text","type":"string"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'referrals'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 2. Denials: Fix insurer lookup to use fhirId instead of database id ───
-- The insurer field uses /api/insurance-companies endpoint which returns InsuranceCompanyDto.
-- valueField was "id" (database ID), but FHIR expects Organization/{fhirId}.
-- InsuranceCompanyDto.fhirId contains the actual FHIR Organization ID.
-- Also fix claims, era-remittance tabs that have the same issue.

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
                 WHERE tab_key IN ('claims', 'claim-denials', 'era-remittance', 'billing')
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
                -- Fix any lookup with /api/insurance-companies using valueField "id"
                IF v_lookup IS NOT NULL
                   AND v_lookup ->> 'endpoint' = '/api/insurance-companies'
                   AND v_lookup ->> 'valueField' = 'id' THEN
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

-- ─── 3. Immunizations: Ensure correct config with all necessary fields ───
-- After V107 (changed coded→select, cvxCode→vaccineCode) and V109 (changed back to coded),
-- the config should have cvxCode as coded type. Ensure status is required and
-- the complete field set is present.

UPDATE tab_field_config
SET field_config = '{
  "sections":[{
    "key":"immunization-info","title":"Immunization Record","columns":2,"collapsible":false,"collapsed":false,
    "fields":[
      {"key":"cvxCode","label":"Vaccine (CVX)","type":"coded","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Immunization","path":"vaccineCode.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/cvx"}},
      {"key":"date","label":"Date Administered","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Immunization","path":"occurrenceDateTime","type":"dateTime"}},
      {"key":"lotNumber","label":"Lot Number","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Immunization","path":"lotNumber","type":"string"}},
      {"key":"site","label":"Administration Site","type":"select","required":false,"colSpan":1,"options":[{"value":"LA","label":"Left Arm"},{"value":"RA","label":"Right Arm"},{"value":"LT","label":"Left Thigh"},{"value":"RT","label":"Right Thigh"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Immunization","path":"site.coding[0].code","type":"code"}},
      {"key":"route","label":"Route","type":"select","required":false,"colSpan":1,"options":[{"value":"IM","label":"Intramuscular"},{"value":"SC","label":"Subcutaneous"},{"value":"ID","label":"Intradermal"},{"value":"PO","label":"Oral"},{"value":"IN","label":"Intranasal"}],"fhirMapping":{"resource":"Immunization","path":"route.coding[0].code","type":"code"}},
      {"key":"doseQuantity","label":"Dose","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"doseQuantity.value","type":"decimal"}},
      {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"showInTable":true,"options":[{"value":"completed","label":"Completed"},{"value":"entered-in-error","label":"Entered in Error"},{"value":"not-done","label":"Not Done"}],"fhirMapping":{"resource":"Immunization","path":"status","type":"code"}},
      {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"Immunization","path":"note[0].text","type":"string"}}
    ]
  }]
}',
    fhir_resources = '[{"type":"Immunization","patientSearchParam":"patient"}]',
    updated_at = now()
WHERE tab_key = 'immunizations'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 4. Rendering Provider: Fix billing provider field to use fhirId ───
-- The provider lookup in billing/claims tabs uses /api/providers endpoint.
-- Ensure valueField is "fhirId" so the reference is valid.

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
                 WHERE tab_key IN ('claims', 'billing', 'era-remittance', 'claim-denials')
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
                -- Fix any provider lookup using valueField "id"
                IF v_lookup IS NOT NULL
                   AND v_lookup ->> 'endpoint' = '/api/providers'
                   AND v_lookup ->> 'valueField' = 'id' THEN
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

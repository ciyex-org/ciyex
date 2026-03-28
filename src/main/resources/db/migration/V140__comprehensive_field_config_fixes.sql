-- V140: Comprehensive fix for all remaining tab_field_config issues
-- Fixes issues 2-18, 22-23 from bug report:
-- - FHIR date type mismatches (date vs dateTime for Period, onset, etc.)
-- - Missing showInTable flags
-- - Missing/incorrect field configs
-- Uses proper JSONB traversal to avoid fragile text REPLACE patterns

-- ══════════════════════════════════════════════════════════════════════════════
-- Helper: Update fhirMapping type for a specific field within a tab
-- ══════════════════════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION fix_fhir_mapping_type(
    p_tab_key TEXT,
    p_field_key TEXT,
    p_new_type TEXT
) RETURNS VOID AS $$
DECLARE
    v_config JSONB;
    v_sections JSONB;
    v_section JSONB;
    v_fields JSONB;
    v_field JSONB;
    v_mapping JSONB;
    v_new_fields JSONB;
    v_new_sections JSONB;
    i INT;
    j INT;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
    IF v_config IS NULL THEN RETURN; END IF;

    v_sections = v_config->'sections';
    IF v_sections IS NULL THEN RETURN; END IF;

    v_new_sections = '[]'::jsonb;
    FOR i IN 0..jsonb_array_length(v_sections)-1 LOOP
        v_section = v_sections->i;
        v_fields = v_section->'fields';
        IF v_fields IS NULL THEN
            v_new_sections = v_new_sections || jsonb_build_array(v_section);
            CONTINUE;
        END IF;
        v_new_fields = '[]'::jsonb;
        FOR j IN 0..jsonb_array_length(v_fields)-1 LOOP
            v_field = v_fields->j;
            IF v_field->>'key' = p_field_key AND v_field->'fhirMapping' IS NOT NULL THEN
                v_mapping = v_field->'fhirMapping' || jsonb_build_object('type', p_new_type);
                v_field = jsonb_set(v_field, '{fhirMapping}', v_mapping);
            END IF;
            v_new_fields = v_new_fields || jsonb_build_array(v_field);
        END LOOP;
        v_section = jsonb_set(v_section, '{fields}', v_new_fields);
        v_new_sections = v_new_sections || jsonb_build_array(v_section);
    END LOOP;

    UPDATE tab_field_config
    SET field_config = jsonb_set(v_config, '{sections}', v_new_sections),
        version = version + 1, updated_at = now()
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
END;
$$ LANGUAGE plpgsql;

-- ══════════════════════════════════════════════════════════════════════════════
-- Helper: Set showInTable for specific fields (idempotent)
-- ══════════════════════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION set_show_in_table(
    p_tab_key TEXT,
    p_field_keys TEXT[]
) RETURNS VOID AS $$
DECLARE
    v_config JSONB;
    v_sections JSONB;
    v_section JSONB;
    v_fields JSONB;
    v_field JSONB;
    v_new_fields JSONB;
    v_new_sections JSONB;
    i INT;
    j INT;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
    IF v_config IS NULL THEN RETURN; END IF;

    v_sections = v_config->'sections';
    IF v_sections IS NULL THEN RETURN; END IF;

    v_new_sections = '[]'::jsonb;
    FOR i IN 0..jsonb_array_length(v_sections)-1 LOOP
        v_section = v_sections->i;
        v_fields = v_section->'fields';
        IF v_fields IS NULL THEN
            v_new_sections = v_new_sections || jsonb_build_array(v_section);
            CONTINUE;
        END IF;
        v_new_fields = '[]'::jsonb;
        FOR j IN 0..jsonb_array_length(v_fields)-1 LOOP
            v_field = v_fields->j;
            IF v_field->>'key' = ANY(p_field_keys) THEN
                v_field = v_field || '{"showInTable": true}'::jsonb;
            END IF;
            v_new_fields = v_new_fields || jsonb_build_array(v_field);
        END LOOP;
        v_section = jsonb_set(v_section, '{fields}', v_new_fields);
        v_new_sections = v_new_sections || jsonb_build_array(v_section);
    END LOOP;

    UPDATE tab_field_config
    SET field_config = jsonb_set(v_config, '{sections}', v_new_sections),
        version = version + 1, updated_at = now()
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
END;
$$ LANGUAGE plpgsql;

-- ══════════════════════════════════════════════════════════════════════════════
-- Helper: Update fhirMapping path for a specific field
-- ══════════════════════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION fix_fhir_mapping_path(
    p_tab_key TEXT,
    p_field_key TEXT,
    p_new_path TEXT
) RETURNS VOID AS $$
DECLARE
    v_config JSONB;
    v_sections JSONB;
    v_section JSONB;
    v_fields JSONB;
    v_field JSONB;
    v_mapping JSONB;
    v_new_fields JSONB;
    v_new_sections JSONB;
    i INT;
    j INT;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
    IF v_config IS NULL THEN RETURN; END IF;

    v_sections = v_config->'sections';
    IF v_sections IS NULL THEN RETURN; END IF;

    v_new_sections = '[]'::jsonb;
    FOR i IN 0..jsonb_array_length(v_sections)-1 LOOP
        v_section = v_sections->i;
        v_fields = v_section->'fields';
        IF v_fields IS NULL THEN
            v_new_sections = v_new_sections || jsonb_build_array(v_section);
            CONTINUE;
        END IF;
        v_new_fields = '[]'::jsonb;
        FOR j IN 0..jsonb_array_length(v_fields)-1 LOOP
            v_field = v_fields->j;
            IF v_field->>'key' = p_field_key AND v_field->'fhirMapping' IS NOT NULL THEN
                v_mapping = v_field->'fhirMapping' || jsonb_build_object('path', p_new_path);
                v_field = jsonb_set(v_field, '{fhirMapping}', v_mapping);
            END IF;
            v_new_fields = v_new_fields || jsonb_build_array(v_field);
        END LOOP;
        v_section = jsonb_set(v_section, '{fields}', v_new_fields);
        v_new_sections = v_new_sections || jsonb_build_array(v_section);
    END LOOP;

    UPDATE tab_field_config
    SET field_config = jsonb_set(v_config, '{sections}', v_new_sections),
        version = version + 1, updated_at = now()
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
END;
$$ LANGUAGE plpgsql;


-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #2: Allergies - severity and onset empty in table
-- Fix: onsetDateTime fhirMapping type should be "dateTime" not "date"
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('allergies', 'startDate', 'dateTime');
SELECT set_show_in_table('allergies', ARRAY['allergyName', 'status', 'severity', 'startDate', 'reaction']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #3: Medical Problems - onset date and resolved date empty
-- V139 should have fixed these but re-apply with JSONB to be safe
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('medicalproblems', 'onsetDate', 'dateTime');
SELECT fix_fhir_mapping_type('medicalproblems', 'abatementDate', 'dateTime');
SELECT set_show_in_table('medicalproblems', ARRAY['conditionName', 'clinicalStatus', 'onsetDate', 'abatementDate', 'severity']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #4: Insurance - effective date and end date empty
-- Fix: Period.start/end require dateTime type, not date
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('insurance-coverage', 'policyEffectiveDate', 'dateTime');
SELECT fix_fhir_mapping_type('insurance-coverage', 'policyEndDate', 'dateTime');
SELECT set_show_in_table('insurance-coverage', ARRAY['insurerName', 'planName', 'subscriberId', 'policyEffectiveDate', 'policyEndDate', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #5: Documents - document date empty
-- Fix: Ensure showInTable and correct type
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('documents', 'date', 'instant');
SELECT set_show_in_table('documents', ARRAY['title', 'category', 'date', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #6: Education - document date empty
-- ══════════════════════════════════════════════════════════════════════════════
SELECT set_show_in_table('education', ARRAY['topic', 'category', 'sent', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #7: Messaging - sent date empty
-- ══════════════════════════════════════════════════════════════════════════════
SELECT set_show_in_table('messaging', ARRAY['subject', 'sender', 'recipient', 'sent', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #8: Appointments - start and end empty
-- Fix: Appointment.start/end are instant type
-- ══════════════════════════════════════════════════════════════════════════════
SELECT set_show_in_table('appointments', ARRAY['appointmentType', 'status', 'start', 'end', 'provider', 'location']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #9: Encounters - start date and end date empty
-- Fix: Period.start/end require dateTime
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('encounters', 'startDate', 'dateTime');
SELECT fix_fhir_mapping_type('encounters', 'endDate', 'dateTime');
SELECT set_show_in_table('encounters', ARRAY['type', 'status', 'startDate', 'endDate', 'provider', 'reason']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #10: Visit Notes - date empty
-- Fix: DocumentReference.date is instant
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('visit-notes', 'date', 'instant');
SELECT set_show_in_table('visit-notes', ARRAY['type', 'author', 'date', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #11: Medications - prescriber column empty
-- Fix: Path "requester" with type "reference" is correct, but ensure
-- the path reads the reference properly. Use "requester.reference"
-- so extractPrimitiveValue gets the reference string for resolution.
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_path('medications', 'prescribingDoctor', 'requester.reference');
SELECT fix_fhir_mapping_type('medications', 'prescribingDoctor', 'reference');
SELECT set_show_in_table('medications', ARRAY['medicationName', 'status', 'dosage', 'prescribingDoctor', 'dateIssued']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #12: Labs - provider column not working
-- Fix: Ensure performer is lookup type with reference fhirMapping
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_path('labs', 'performer', 'performer[0].reference');
SELECT fix_fhir_mapping_type('labs', 'performer', 'reference');
SELECT set_show_in_table('labs', ARRAY['testName', 'testCode', 'status', 'effectiveDate', 'performer']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #13: Procedures - CPT code and date performed empty / 400 error
-- Fix: performedDateTime type, ensure showInTable
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('procedures', 'performedDate', 'dateTime');
SELECT set_show_in_table('procedures', ARRAY['procedureName', 'status', 'cptCode', 'performedDate', 'performer']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #14: Claims - created date, service from/to empty
-- Fix: billablePeriod.start/end need dateTime type
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('claims', 'billablePeriodStart', 'dateTime');
SELECT fix_fhir_mapping_type('claims', 'billablePeriodEnd', 'dateTime');
SELECT set_show_in_table('claims', ARRAY['type', 'status', 'created', 'billablePeriodStart', 'billablePeriodEnd', 'total']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #15: Claim Submissions - submission date empty
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('claim-submissions', 'billablePeriodStart', 'dateTime');
SELECT fix_fhir_mapping_type('claim-submissions', 'billablePeriodEnd', 'dateTime');
SELECT set_show_in_table('claim-submissions', ARRAY['type', 'status', 'created', 'total', 'trackingNumber']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #16: Claim Denials - ClaimResponse.request reference error
-- Fix: V136 broke reference types by replacing ALL "type":"reference" with "string"
-- Restore proper reference types for insurer and provider fields
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('claim-denials', 'insurer', 'reference');
SELECT fix_fhir_mapping_type('claim-denials', 'provider', 'reference');
-- The request field needs: UI type "lookup" with lookupConfig for claims,
-- fhirMapping path "request.reference" with type "reference"
SELECT fix_fhir_mapping_path('claim-denials', 'request', 'request.reference');
SELECT fix_fhir_mapping_type('claim-denials', 'request', 'reference');
-- Also restore the lookup UI type and config for request field
DO $$
DECLARE
    v_config JSONB;
    v_sections JSONB;
    v_section JSONB;
    v_fields JSONB;
    v_field JSONB;
    v_new_fields JSONB;
    v_new_sections JSONB;
    i INT;
    j INT;
BEGIN
    SELECT field_config INTO v_config
    FROM tab_field_config
    WHERE tab_key = 'claim-denials' AND org_id = '*' AND practice_type_code = '*';
    IF v_config IS NULL THEN RETURN; END IF;

    v_sections = v_config->'sections';
    IF v_sections IS NULL THEN RETURN; END IF;

    v_new_sections = '[]'::jsonb;
    FOR i IN 0..jsonb_array_length(v_sections)-1 LOOP
        v_section = v_sections->i;
        v_fields = v_section->'fields';
        IF v_fields IS NULL THEN
            v_new_sections = v_new_sections || jsonb_build_array(v_section);
            CONTINUE;
        END IF;
        v_new_fields = '[]'::jsonb;
        FOR j IN 0..jsonb_array_length(v_fields)-1 LOOP
            v_field = v_fields->j;
            IF v_field->>'key' = 'request' THEN
                v_field = jsonb_set(v_field, '{type}', '"lookup"');
                v_field = v_field || '{"lookupConfig":{"endpoint":"/api/fhir-resource/claims","displayField":"id","valueField":"id","searchable":true}}'::jsonb;
            END IF;
            v_new_fields = v_new_fields || jsonb_build_array(v_field);
        END LOOP;
        v_section = jsonb_set(v_section, '{fields}', v_new_fields);
        v_new_sections = v_new_sections || jsonb_build_array(v_section);
    END LOOP;

    UPDATE tab_field_config
    SET field_config = jsonb_set(v_config, '{sections}', v_new_sections),
        version = version + 1, updated_at = now()
    WHERE tab_key = 'claim-denials' AND org_id = '*' AND practice_type_code = '*';
END $$;
SELECT set_show_in_table('claim-denials', ARRAY['type', 'status', 'outcome', 'created', 'totalSubmitted', 'paymentAmount']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #17: ERA - created date empty
-- ══════════════════════════════════════════════════════════════════════════════
SELECT set_show_in_table('era-remittance', ARRAY['type', 'status', 'created', 'paymentAmount', 'paymentDate', 'paymentIdentifier']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #18: Transactions - date column missing
-- ══════════════════════════════════════════════════════════════════════════════
SELECT set_show_in_table('transactions', ARRAY['description', 'serviceDate', 'amount', 'status']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #22: Issues page - onset date empty, empty rows
-- Fix: onsetDateTime type, ensure key fields have showInTable
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('issues', 'onsetDate', 'dateTime');
SELECT set_show_in_table('issues', ARRAY['conditionName', 'status', 'onsetDate', 'severity', 'category']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Issue #23: Reports page - report date empty
-- Fix: MeasureReport.date is dateTime, add showInTable to all key fields
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('report', 'date', 'dateTime');
SELECT set_show_in_table('report', ARRAY['reportType', 'status', 'date', 'reportName']);

-- ══════════════════════════════════════════════════════════════════════════════
-- Also fix claim-submissions: V136 broke reference types here too
-- ══════════════════════════════════════════════════════════════════════════════
SELECT fix_fhir_mapping_type('claim-submissions', 'insurer', 'reference');
SELECT fix_fhir_mapping_type('claim-submissions', 'provider', 'reference');

-- ══════════════════════════════════════════════════════════════════════════════
-- Clean up helper functions
-- ══════════════════════════════════════════════════════════════════════════════
DROP FUNCTION IF EXISTS fix_fhir_mapping_type(TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS set_show_in_table(TEXT, TEXT[]);
DROP FUNCTION IF EXISTS fix_fhir_mapping_path(TEXT, TEXT, TEXT);

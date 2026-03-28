-- V138: Comprehensive fix for showInTable flags across all tabs
-- Previous V132-V137 migrations used fragile REPLACE string matching that failed
-- due to JSON key ordering differences. This migration uses proper JSONB operations.
--
-- Fixes reported issues:
-- 1. Allergy severity/onset empty in table
-- 2. Problem onset/resolved dates missing
-- 3. Insurance dates empty (already has showInTable but verify)
-- 4. Document date missing
-- 5. Education date missing
-- 6. Encounter start/end dates missing
-- 7. Medication prescriber empty
-- 8. Labs provider empty
-- 9. Procedures CPT/date empty
-- 10. Claims dates missing
-- 11. Submissions date missing
-- 12. ERA created date missing
-- 13. Transactions date missing
-- 14. Denials dates missing

-- Helper function to add showInTable:true to specific fields by key
CREATE OR REPLACE FUNCTION add_show_in_table(
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

    IF v_config IS NULL THEN
        RAISE NOTICE 'No config found for tab_key: %', p_tab_key;
        RETURN;
    END IF;

    v_sections = v_config->'sections';
    IF v_sections IS NULL THEN RETURN; END IF;

    v_new_sections = '[]'::jsonb;
    FOR i IN 0..jsonb_array_length(v_sections)-1 LOOP
        v_section = v_sections->i;
        v_fields = v_section->'fields';
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
        updated_at = now()
    WHERE tab_key = p_tab_key AND org_id = '*' AND practice_type_code = '*';
END;
$$ LANGUAGE plpgsql;

-- ── Allergies: show allergyName, status, severity, startDate ──
SELECT add_show_in_table('allergies', ARRAY['allergyName', 'status', 'severity', 'startDate', 'reaction']);

-- ── Medical Problems: show conditionName, clinicalStatus, onsetDate, abatementDate, severity ──
SELECT add_show_in_table('medicalproblems', ARRAY['conditionName', 'clinicalStatus', 'onsetDate', 'abatementDate', 'severity']);

-- ── Documents: show title, category, date, status ──
SELECT add_show_in_table('documents', ARRAY['title', 'category', 'date', 'status']);

-- ── Education: show topic, category, sent, status ──
SELECT add_show_in_table('education', ARRAY['topic', 'category', 'sent', 'status']);

-- ── Encounters: show type, status, startDate, endDate, provider, reason ──
SELECT add_show_in_table('encounters', ARRAY['type', 'status', 'startDate', 'endDate', 'provider', 'reason']);

-- ── Medications: show medicationName, status, dosage, prescribingDoctor, dateIssued ──
SELECT add_show_in_table('medications', ARRAY['medicationName', 'status', 'dosage', 'prescribingDoctor', 'dateIssued']);

-- ── Labs: show testName, testCode, status, effectiveDate, performer ──
SELECT add_show_in_table('labs', ARRAY['testName', 'testCode', 'status', 'effectiveDate', 'performer']);

-- ── Procedures: show procedureName, status, cptCode, performedDate, performer ──
SELECT add_show_in_table('procedures', ARRAY['procedureName', 'status', 'cptCode', 'performedDate', 'performer']);

-- ── Claims: show type, status, created, billablePeriodStart, billablePeriodEnd, total ──
SELECT add_show_in_table('claims', ARRAY['type', 'status', 'created', 'billablePeriodStart', 'billablePeriodEnd', 'total']);

-- ── Claim Submissions: show type, status, created, total, trackingNumber ──
SELECT add_show_in_table('claim-submissions', ARRAY['type', 'status', 'created', 'total', 'trackingNumber']);

-- ── Claim Denials: show type, status, outcome, created, totalSubmitted, paymentAmount ──
SELECT add_show_in_table('claim-denials', ARRAY['type', 'status', 'outcome', 'created', 'totalSubmitted', 'paymentAmount']);

-- ── ERA Remittance: show type, status, created, paymentAmount, paymentDate, paymentIdentifier ──
SELECT add_show_in_table('era-remittance', ARRAY['type', 'status', 'created', 'paymentAmount', 'paymentDate', 'paymentIdentifier']);

-- ── Transactions: show description, serviceDate, amount, status ──
SELECT add_show_in_table('transactions', ARRAY['description', 'serviceDate', 'amount', 'status']);

-- Clean up helper function
DROP FUNCTION IF EXISTS add_show_in_table(TEXT, TEXT[]);

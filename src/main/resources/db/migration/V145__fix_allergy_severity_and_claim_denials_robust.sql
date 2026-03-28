-- V145: Robust fix for allergy severity FHIR path and claim-denials request field
-- V133 used text REPLACE which fails when JSON whitespace differs from the match string.
-- This migration uses a PL/pgSQL function to walk the JSON structure reliably.

-- ─── 1. Fix allergies severity: criticality → reaction[0].severity ───
-- Walk through field_config.sections[*].fields[*] and fix the fhirMapping.path
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    mapping jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'allergies' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' = 'severity' THEN
                    mapping := field->'fhirMapping';
                    IF mapping IS NOT NULL AND mapping->>'path' = 'criticality' THEN
                        new_config := jsonb_set(
                            new_config,
                            ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'fhirMapping', 'path'],
                            '"reaction[0].severity"'
                        );
                    END IF;
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 2. Fix claim-denials/claim-submissions: request field → lookup type ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
    new_field jsonb;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key IN ('claim-denials', 'claim-submissions') LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' = 'request' AND (field->>'type' = 'text' OR field->'fhirMapping'->>'type' = 'string') THEN
                    -- Replace the entire field with a proper lookup config
                    new_field := jsonb_build_object(
                        'key', 'request',
                        'label', 'Original Claim',
                        'type', 'lookup',
                        'required', false,
                        'colSpan', 1,
                        'showInTable', true,
                        'lookupConfig', jsonb_build_object(
                            'endpoint', '/api/fhir/Claim',
                            'displayField', 'id',
                            'valueField', 'id',
                            'searchable', false
                        ),
                        'fhirMapping', jsonb_build_object(
                            'resource', 'ClaimResponse',
                            'path', 'request.reference',
                            'type', 'reference'
                        )
                    );
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text],
                        new_field
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 3. Ensure encounters startDate/endDate have showInTable=true ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'encounters' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' IN ('startDate', 'endDate', 'provider', 'type', 'reason', 'status') THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'showInTable'],
                        'true'
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 4. Ensure transactions serviceDate/amount have showInTable=true and correct type ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'transactions' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' IN ('serviceDate', 'amount', 'description', 'status', 'transactionType') THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'showInTable'],
                        'true'
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 5. Ensure billing serviceDate/cptCode/provider have showInTable=true ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'billing' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' IN ('serviceDate', 'cptCode', 'provider', 'amount', 'status', 'diagnosisCode', 'totalAmount') THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'showInTable'],
                        'true'
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 6. Ensure labs performer has showInTable=true ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'labs' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' IN ('performer', 'effectiveDate', 'testName', 'status', 'testCode') THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'showInTable'],
                        'true'
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- ─── 7. Ensure appointments start/end have showInTable=true ───
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'appointments' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' IN ('start', 'end', 'status', 'appointmentType', 'reason', 'provider', 'patient') THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'showInTable'],
                        'true'
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

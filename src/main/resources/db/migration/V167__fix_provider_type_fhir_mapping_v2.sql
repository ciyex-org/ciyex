-- Fix providerType fhirMapping: V166 REPLACE failed due to jsonb spacing mismatch.
-- field_config structure: {"sections": [{"key": "...", "fields": [{...}]}]}
-- Use PL/pgSQL to find and update the nested fhirMapping for providerType.

DO $$
DECLARE
    rec RECORD;
    section_idx INT;
    field_idx INT;
    sections jsonb;
    fields jsonb;
    field_obj jsonb;
    new_mapping jsonb;
    updated_config jsonb;
BEGIN
    new_mapping := '{"path": "qualification[0].code.coding[1].code", "resource": "Practitioner", "system": "http://ciyex.org/CodeSystem/provider-type", "type": "code"}'::jsonb;

    FOR rec IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'providers'
    LOOP
        sections := rec.field_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR section_idx IN 0..jsonb_array_length(sections) - 1
        LOOP
            fields := sections->section_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR field_idx IN 0..jsonb_array_length(fields) - 1
            LOOP
                field_obj := fields->field_idx;
                IF field_obj->>'key' = 'professionalDetails.providerType' THEN
                    -- Update the fhirMapping at this exact path
                    updated_config := jsonb_set(
                        rec.field_config,
                        array['sections', section_idx::text, 'fields', field_idx::text, 'fhirMapping'],
                        new_mapping
                    );
                    UPDATE tab_field_config SET field_config = updated_config WHERE id = rec.id;
                    RAISE NOTICE 'Updated providerType fhirMapping for tab_field_config id=%', rec.id;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

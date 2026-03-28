-- V172: Add Allergen field to allergies tab and rename allergyName label to "Allergy"
-- allergyName (code.text) = allergy name/condition → relabeled "Allergy"
-- New allergen field (reaction[0].substance.text) = specific triggering substance → labeled "Allergen"

DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    new_field jsonb;
    s_idx int;
    f_idx int;
    allergen_exists boolean;
    allergy_name_idx int;
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'allergies' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            allergen_exists := false;
            allergy_name_idx := -1;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' = 'allergen' THEN
                    allergen_exists := true;
                END IF;
                IF field->>'key' = 'allergyName' THEN
                    allergy_name_idx := f_idx;
                END IF;
            END LOOP;

            -- Rename allergyName label from "Allergen" to "Allergy"
            IF allergy_name_idx >= 0 THEN
                new_config := jsonb_set(
                    new_config,
                    ARRAY['sections', s_idx::text, 'fields', allergy_name_idx::text, 'label'],
                    '"Allergy"'
                );
                new_config := jsonb_set(
                    new_config,
                    ARRAY['sections', s_idx::text, 'fields', allergy_name_idx::text, 'placeholder'],
                    '"Allergy name"'
                );
            END IF;

            -- Insert allergen field right after allergyName
            IF NOT allergen_exists AND allergy_name_idx >= 0 THEN
                new_field := jsonb_build_object(
                    'key', 'allergen',
                    'label', 'Allergen',
                    'type', 'text',
                    'required', false,
                    'colSpan', 1,
                    'placeholder', 'Specific allergen/substance',
                    'showInTable', false,
                    'fhirMapping', jsonb_build_object(
                        'resource', 'AllergyIntolerance',
                        'path', 'reaction[0].substance.text',
                        'type', 'string'
                    )
                );
                new_config := jsonb_insert(
                    new_config,
                    ARRAY['sections', s_idx::text, 'fields', allergy_name_idx::text],
                    new_field,
                    true  -- insert after allergyName
                );
            END IF;

        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

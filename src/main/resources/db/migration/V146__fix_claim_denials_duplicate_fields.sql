-- V146: Remove duplicate fields from claim-denials tab
-- V141 added both 'originalClaimRef' and 'responseDate' as new fields,
-- but V145 already converted the existing 'request' field to a proper lookup.
-- This leaves duplicates: originalClaimRef + request (same concept), and possibly duplicate responseDate.

DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    new_fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
    seen_keys text[];
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'claim-denials' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            -- Remove 'originalClaimRef' field (superseded by 'request' lookup)
            -- Also deduplicate any fields with the same key
            new_fields := '[]'::jsonb;
            seen_keys := ARRAY[]::text[];

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                -- Skip originalClaimRef (superseded by request lookup)
                IF field->>'key' = 'originalClaimRef' THEN
                    CONTINUE;
                END IF;
                -- Skip duplicate keys (keep first occurrence)
                IF field->>'key' = ANY(seen_keys) THEN
                    CONTINUE;
                END IF;
                seen_keys := seen_keys || (field->>'key');
                new_fields := new_fields || jsonb_build_array(field);
            END LOOP;

            new_config := jsonb_set(
                new_config,
                ARRAY['sections', s_idx::text, 'fields'],
                new_fields
            );
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- Also fix claim-submissions if it has the same duplicates
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    new_fields jsonb;
    field jsonb;
    s_idx int;
    f_idx int;
    seen_keys text[];
BEGIN
    FOR r IN SELECT id, field_config FROM tab_field_config WHERE tab_key = 'claim-submissions' LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            new_fields := '[]'::jsonb;
            seen_keys := ARRAY[]::text[];

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                IF field->>'key' = 'originalClaimRef' THEN CONTINUE; END IF;
                IF field->>'key' = ANY(seen_keys) THEN CONTINUE; END IF;
                seen_keys := seen_keys || (field->>'key');
                new_fields := new_fields || jsonb_build_array(field);
            END LOOP;

            new_config := jsonb_set(
                new_config,
                ARRAY['sections', s_idx::text, 'fields'],
                new_fields
            );
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

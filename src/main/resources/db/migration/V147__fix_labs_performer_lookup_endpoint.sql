-- V147: Fix labs performer lookupConfig — has "tabKey" instead of "endpoint"
-- The lookup URL was undefined because the config had {tabKey: "providers"} not {endpoint: "/api/providers"}

DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    lc jsonb;
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
                lc := field->'lookupConfig';
                -- Fix lookupConfig that has tabKey but no endpoint
                IF lc IS NOT NULL AND lc->>'tabKey' IS NOT NULL AND lc->>'endpoint' IS NULL THEN
                    -- Replace tabKey with proper endpoint
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'lookupConfig'],
                        (lc - 'tabKey') || jsonb_build_object('endpoint', '/api/' || (lc->>'tabKey'))
                    );
                END IF;
            END LOOP;
        END LOOP;

        UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
    END LOOP;
END $$;

-- Also fix any other tabs that might have the same issue (tabKey instead of endpoint)
DO $$
DECLARE
    r RECORD;
    new_config jsonb;
    sections jsonb;
    fields jsonb;
    field jsonb;
    lc jsonb;
    s_idx int;
    f_idx int;
BEGIN
    FOR r IN SELECT id, tab_key, field_config FROM tab_field_config LOOP
        new_config := r.field_config;
        sections := new_config->'sections';
        IF sections IS NULL THEN CONTINUE; END IF;

        FOR s_idx IN 0..jsonb_array_length(sections)-1 LOOP
            fields := sections->s_idx->'fields';
            IF fields IS NULL THEN CONTINUE; END IF;

            FOR f_idx IN 0..jsonb_array_length(fields)-1 LOOP
                field := fields->f_idx;
                lc := field->'lookupConfig';
                IF lc IS NOT NULL AND lc->>'tabKey' IS NOT NULL AND lc->>'endpoint' IS NULL THEN
                    new_config := jsonb_set(
                        new_config,
                        ARRAY['sections', s_idx::text, 'fields', f_idx::text, 'lookupConfig'],
                        (lc - 'tabKey') || jsonb_build_object('endpoint', '/api/' || (lc->>'tabKey'))
                    );
                END IF;
            END LOOP;
        END LOOP;

        IF new_config != r.field_config THEN
            UPDATE tab_field_config SET field_config = new_config, updated_at = now() WHERE id = r.id;
        END IF;
    END LOOP;
END $$;

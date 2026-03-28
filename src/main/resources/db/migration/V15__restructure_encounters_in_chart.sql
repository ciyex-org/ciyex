-- Restructure chart tabs:
-- 1. Move Appointments and Referrals from Encounters group to Overview
-- 2. Encounters group keeps only: Encounters, Visit Notes (managed via Encounter settings page)

-- Update ALL tab_config rows (universal default + any practice type defaults)
UPDATE tab_config
SET tab_config = (
    SELECT jsonb_agg(
        CASE
            -- Overview: append Appointments and Referrals
            WHEN cat->>'label' = 'Overview' THEN
                jsonb_set(
                    cat,
                    '{tabs}',
                    (cat->'tabs') ||
                    '[{"key":"appointments","icon":"Clock","label":"Appointments","visible":true,"position":10},{"key":"referrals","icon":"Share2","label":"Referrals","visible":true,"position":11}]'::jsonb
                )
            -- Encounters: keep only encounters + visit-notes
            WHEN cat->>'label' = 'Encounters' THEN
                jsonb_set(
                    cat,
                    '{tabs}',
                    (SELECT COALESCE(jsonb_agg(t ORDER BY (t->>'position')::int), '[]'::jsonb)
                     FROM jsonb_array_elements(cat->'tabs') t
                     WHERE t->>'key' IN ('encounters', 'visit-notes'))
                )
            ELSE cat
        END
        ORDER BY (cat->>'position')::int
    )
    FROM jsonb_array_elements(tab_config) cat
),
updated_at = now();

-- Reindex positions within Overview tabs
UPDATE tab_config
SET tab_config = (
    SELECT jsonb_agg(
        CASE
            WHEN cat->>'label' = 'Overview' THEN
                jsonb_set(
                    cat,
                    '{tabs}',
                    (SELECT jsonb_agg(
                        jsonb_set(t, '{position}', to_jsonb(rn - 1))
                     )
                     FROM (
                        SELECT t, ROW_NUMBER() OVER () as rn
                        FROM jsonb_array_elements(cat->'tabs') t
                     ) sub)
                )
            ELSE cat
        END
        ORDER BY (cat->>'position')::int
    )
    FROM jsonb_array_elements(tab_config) cat
),
updated_at = now();

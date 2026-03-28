-- V49: Seed demo-care-gaps plugin installation for dev/demo environments.
-- This allows the demo plugin to render immediately without requiring
-- a marketplace subscription.
--
-- Strategy: Collect known org_ids from menu_org_override (orgs that have
-- customized their sidebar), plus the well-known dev org 'hinisoft'.
-- In production, installations are created automatically via marketplace webhook.

INSERT INTO app_installations (id, org_id, app_id, app_slug, app_name, app_icon_url, app_category,
                               status, config, extension_points, installed_by, installed_at, updated_at)
SELECT
    gen_random_uuid(),
    orgs.org_id,
    '10000000-0000-0000-0000-000000000010'::uuid,
    'demo-care-gaps',
    'Care Gaps (Demo)',
    'https://cdn.ciyex.com/icons/care-gaps.svg',
    'CLINICAL',
    'active',
    '{"alert_threshold": "ALL"}'::jsonb,
    '["patient-chart:banner-alert", "patient-chart:tab"]'::jsonb,
    'system-seed',
    now(),
    now()
FROM (
    -- Known dev/demo org aliases
    SELECT 'hinisoft' AS org_id
    UNION
    -- Any orgs that have customized their sidebar (real orgs in the DB)
    SELECT DISTINCT org_id FROM menu_org_override WHERE org_id IS NOT NULL
) orgs
WHERE NOT EXISTS (
    SELECT 1 FROM app_installations ai
    WHERE ai.org_id = orgs.org_id AND ai.app_slug = 'demo-care-gaps'
);

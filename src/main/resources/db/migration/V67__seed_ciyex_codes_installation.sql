-- V67: Seed ciyex-codes (medical code reference) app installation for dev/demo environments.
-- In production, installations are created automatically via marketplace webhook.

INSERT INTO app_installations (id, org_id, app_id, app_slug, app_name, app_icon_url, app_category,
                               status, config, extension_points, installed_by, installed_at, updated_at)
SELECT
    gen_random_uuid(),
    orgs.org_id,
    '10000000-0000-0000-0000-000000000005'::uuid,
    'ciyex-codes',
    'Ciyex Codes',
    'https://cdn.ciyex.com/icons/ciyex-codes.svg',
    'INFRASTRUCTURE',
    'active',
    '{"service_url": "http://ciyex-codes.ciyex-codes.svc.cluster.local:8084"}'::jsonb,
    '[]'::jsonb,
    'system-seed',
    now(),
    now()
FROM (
    SELECT 'hinisoft' AS org_id
    UNION
    SELECT DISTINCT org_id FROM menu_org_override WHERE org_id IS NOT NULL
) orgs
WHERE NOT EXISTS (
    SELECT 1 FROM app_installations ai
    WHERE ai.org_id = orgs.org_id AND ai.app_slug = 'ciyex-codes'
);

-- V175: Ensure ALL orgs have ciyex-codes installation.
-- The ciyex-codes service is shared infrastructure (medical code reference)
-- and should be available to every tenant, not just those that existed at V67/V92 time.

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
    '{"service_url": "http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"}'::jsonb,
    '["encounter:sidebar"]'::jsonb,
    'system-seed',
    now(),
    now()
FROM (
    -- All known orgs from various tables
    SELECT DISTINCT org_id FROM menu_org_override WHERE org_id IS NOT NULL
    UNION
    SELECT DISTINCT org_alias AS org_id FROM clinical_task WHERE org_alias IS NOT NULL
    UNION
    SELECT DISTINCT org_id FROM app_installations WHERE org_id IS NOT NULL
    UNION
    -- Common dev orgs that may not appear in above tables
    SELECT 'ehr-sunflow'
    UNION
    SELECT 'hinisoft'
    UNION
    SELECT 'hinisoft-dev'
    UNION
    SELECT 'sunrise-family-medicine'
    UNION
    SELECT 'test-clinic-1772851243'
) orgs
WHERE NOT EXISTS (
    SELECT 1 FROM app_installations ai
    WHERE ai.org_id = orgs.org_id AND ai.app_slug = 'ciyex-codes'
);

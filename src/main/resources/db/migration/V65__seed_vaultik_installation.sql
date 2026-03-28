-- V65: Seed Vaultik (file storage) plugin installation for dev/demo environments.
-- In production, installations are created automatically via marketplace webhook.

INSERT INTO app_installations (id, org_id, app_id, app_slug, app_name, app_icon_url, app_category,
                               status, config, extension_points, installed_by, installed_at, updated_at)
SELECT
    gen_random_uuid(),
    orgs.org_id,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'vaultik',
    'Vaultik',
    'https://cdn.ciyex.com/icons/vaultik.svg',
    'INFRASTRUCTURE',
    'active',
    '{"storage_mode": "files-service", "max_file_size_mb": 50, "service_url": "http://ciyex-files.ciyex-files.svc.cluster.local:8080"}'::jsonb,
    '["settings:nav-item", "patient-chart:tab"]'::jsonb,
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
    WHERE ai.org_id = orgs.org_id AND ai.app_slug = 'vaultik'
);

-- Insert telehealth app installation if missing (V88 may have conflicted)
-- Also ensure RCM has settings:nav-item

INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000006', 'ciyex-telehealth', 'Ciyex Telehealth', 'TELEHEALTH',
     'active',
     '{"vendor_name":"Doxy.me","vendor_id":"doxy-001","default_duration":15,"require_waiting_room":true,"e2e_encryption":true,"hipaa_baa_signed":true}'::jsonb,
     '["settings:nav-item","patient-chart:action-bar"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000006', 'ciyex-telehealth', 'Ciyex Telehealth', 'TELEHEALTH',
     'active',
     '{"vendor_name":"Doxy.me","vendor_id":"doxy-001","default_duration":15,"require_waiting_room":true,"e2e_encryption":true,"hipaa_baa_signed":true}'::jsonb,
     '["settings:nav-item","patient-chart:action-bar"]'::jsonb)
ON CONFLICT (org_id, app_slug) DO UPDATE SET
    status = 'active',
    config = EXCLUDED.config,
    extension_points = EXCLUDED.extension_points,
    updated_at = NOW();

-- RCM: add settings:nav-item to extension_points
UPDATE app_installations
SET extension_points = '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb,
    config = config || '{"vendor_name":"Waystar","clearinghouse_name":"Availity"}'::jsonb
WHERE app_slug = 'ciyex-rcm'
  AND NOT extension_points @> '"settings:nav-item"'::jsonb;

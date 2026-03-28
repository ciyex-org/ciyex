-- V131: Replace ciyex-notifications and ciyex-fax with twilio-sms and efax apps
-- These are now separate microservices instead of the monolithic ciyex-comm

-- ── Remove old ciyex-notifications installations ──
DELETE FROM app_installations WHERE app_slug = 'ciyex-notifications';

-- ── Remove old ciyex-fax installations ──
DELETE FROM app_installations WHERE app_slug = 'ciyex-fax';

-- ── Insert Twilio SMS installations ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000016', 'twilio-sms', 'Twilio SMS', 'COMMUNICATION',
     'active',
     '{"service_url":"http://twilio-sms.twilio-sms.svc.cluster.local:8080"}'::jsonb,
     '["settings:nav-item"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000016', 'twilio-sms', 'Twilio SMS', 'COMMUNICATION',
     'active',
     '{"service_url":"http://twilio-sms.twilio-sms.svc.cluster.local:8080"}'::jsonb,
     '["settings:nav-item"]'::jsonb)
ON CONFLICT DO NOTHING;

-- ── Insert eFax installations ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000015', 'efax', 'eFax', 'COMMUNICATION',
     'active',
     '{"service_url":"http://efax.efax.svc.cluster.local:8080"}'::jsonb,
     '["settings:nav-item","encounter:toolbar"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000015', 'efax', 'eFax', 'COMMUNICATION',
     'active',
     '{"service_url":"http://efax.efax.svc.cluster.local:8080"}'::jsonb,
     '["settings:nav-item","encounter:toolbar"]'::jsonb)
ON CONFLICT DO NOTHING;

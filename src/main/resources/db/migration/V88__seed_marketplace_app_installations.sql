-- V88: Seed app installations for dev orgs
-- These represent marketplace apps that are pre-installed for development/testing.
-- In production, installations happen via marketplace webhook on subscription.created.

-- ── Ask Ciya (AI Assistant) ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category, status, extension_points, config)
VALUES
  ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000009', 'ask-ciya', 'Ask Ciya - AI Assistant', 'AI', 'active',
   '["patient-chart:tab", "encounter:toolbar"]'::jsonb, '{}'::jsonb),
  ('hinisoft', '10000000-0000-0000-0000-000000000009', 'ask-ciya', 'Ask Ciya - AI Assistant', 'AI', 'active',
   '["patient-chart:tab", "encounter:toolbar"]'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

-- ── Ciyex Telehealth ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category, status, extension_points, config)
VALUES
  ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000006', 'ciyex-telehealth', 'Ciyex Telehealth', 'TELEHEALTH', 'active',
   '["patient-chart:action-bar"]'::jsonb, '{}'::jsonb),
  ('hinisoft', '10000000-0000-0000-0000-000000000006', 'ciyex-telehealth', 'Ciyex Telehealth', 'TELEHEALTH', 'active',
   '["patient-chart:action-bar"]'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

-- ── Ciyex RCM ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category, status, extension_points, config)
VALUES
  ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000007', 'ciyex-rcm', 'Ciyex RCM', 'RCM', 'active',
   '["encounter:form-footer", "patient-chart:tab"]'::jsonb, '{}'::jsonb),
  ('hinisoft', '10000000-0000-0000-0000-000000000007', 'ciyex-rcm', 'Ciyex RCM', 'RCM', 'active',
   '["encounter:form-footer", "patient-chart:tab"]'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

-- ── Ciyex Credentialing ──
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category, status, extension_points, config)
VALUES
  ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000003', 'ciyex-credentialing', 'Ciyex Credentialing', 'OPERATIONS', 'active',
   '["settings:nav-item"]'::jsonb, '{}'::jsonb),
  ('hinisoft', '10000000-0000-0000-0000-000000000003', 'ciyex-credentialing', 'Ciyex Credentialing', 'OPERATIONS', 'active',
   '["settings:nav-item"]'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

-- ── Update ciyex-codes extension_points (was empty) ──
UPDATE app_installations
SET extension_points = '["encounter:sidebar"]'::jsonb
WHERE app_slug = 'ciyex-codes' AND extension_points = '[]'::jsonb;

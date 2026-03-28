-- V92: Fix ciyex-codes service URL port (8084 → 8080) to match actual K8s service.
-- Also ensure all dev orgs have the ciyex-codes installation.

-- 1. Fix port in existing installations
UPDATE app_installations
SET config = jsonb_set(config, '{service_url}', '"http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"'),
    updated_at = now()
WHERE app_slug = 'ciyex-codes'
  AND config->>'service_url' LIKE '%:8084%';

-- 2. Ensure dev orgs have the codes installation (same pattern as V88)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category, status, config, extension_points)
VALUES
  ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000005', 'ciyex-codes', 'Ciyex Codes', 'INFRASTRUCTURE', 'active',
   '{"service_url": "http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"}'::jsonb, '["encounter:sidebar"]'::jsonb),
  ('hinisoft', '10000000-0000-0000-0000-000000000005', 'ciyex-codes', 'Ciyex Codes', 'INFRASTRUCTURE', 'active',
   '{"service_url": "http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"}'::jsonb, '["encounter:sidebar"]'::jsonb)
ON CONFLICT DO NOTHING;

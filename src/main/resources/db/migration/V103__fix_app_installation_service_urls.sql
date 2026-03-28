-- Fix app installation service URLs for ciyex-rcm and other services
-- These were seeded without service_url in config, causing app-proxy 404

UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-rcm.ciyex-rcm.svc.cluster.local:8080"}'::jsonb
WHERE app_slug = 'ciyex-rcm'
  AND (config->>'service_url' IS NULL OR config->>'service_url' = '');

UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"}'::jsonb
WHERE app_slug = 'ciyex-codes'
  AND (config->>'service_url' IS NULL OR config->>'service_url' = '');

UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-files.ciyex-files.svc.cluster.local:8080"}'::jsonb
WHERE app_slug IN ('ciyex-files', 'vaultik')
  AND (config->>'service_url' IS NULL OR config->>'service_url' = '');

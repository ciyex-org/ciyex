-- V66: Add service_url to Vaultik app installation config.
-- The service URL is provided by the marketplace when apps are installed via webhook.
-- For existing seed installations, set the K8s internal service URL.

UPDATE app_installations
SET config = config || '{"service_url": "http://ciyex-files.ciyex-files.svc.cluster.local:8080"}'::jsonb
WHERE app_slug = 'vaultik'
  AND (config ->> 'service_url') IS NULL;

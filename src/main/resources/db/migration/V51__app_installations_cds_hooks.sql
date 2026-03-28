-- V51: Add CDS Hooks discovery URL to app_installations
-- When an app supports CDS Hooks, the discovery URL is stored with each installation
-- so the EHR can discover and invoke CDS services per-org.

ALTER TABLE app_installations ADD COLUMN IF NOT EXISTS cds_hooks_discovery_url TEXT;
ALTER TABLE app_installations ADD COLUMN IF NOT EXISTS supported_hooks JSONB DEFAULT '[]';

CREATE INDEX IF NOT EXISTS idx_app_installations_cds_hooks
    ON app_installations(org_id) WHERE cds_hooks_discovery_url IS NOT NULL AND status = 'active';

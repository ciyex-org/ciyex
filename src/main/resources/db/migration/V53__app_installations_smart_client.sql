-- Add SMART on FHIR client registration fields to app_installations.
-- When a SMART app is installed, ciyex-api registers a Keycloak OAuth2 client
-- and stores the client ID here for launch-time reference.

ALTER TABLE app_installations
    ADD COLUMN IF NOT EXISTS smart_launch_url TEXT,
    ADD COLUMN IF NOT EXISTS smart_redirect_uris JSONB DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS fhir_scopes JSONB DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS keycloak_client_id VARCHAR(255);

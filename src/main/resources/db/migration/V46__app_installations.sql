-- V46: App installations registry for Ciyex Hub marketplace integration
-- Tracks which marketplace apps are installed per org

CREATE TABLE app_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id VARCHAR(100) NOT NULL,
    app_id UUID NOT NULL,
    app_slug VARCHAR(100) NOT NULL,
    app_name VARCHAR(255) NOT NULL,
    app_icon_url TEXT,
    app_category VARCHAR(50),
    subscription_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    config JSONB DEFAULT '{}',
    installed_by VARCHAR(255),
    installed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    uninstalled_at TIMESTAMP,
    UNIQUE(org_id, app_slug)
);

CREATE INDEX idx_app_installations_org ON app_installations(org_id);
CREATE INDEX idx_app_installations_status ON app_installations(org_id, status);

-- Tracks app launches for HIPAA audit
CREATE TABLE app_launch_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id VARCHAR(100) NOT NULL,
    app_installation_id UUID REFERENCES app_installations(id),
    app_slug VARCHAR(100) NOT NULL,
    launched_by VARCHAR(255) NOT NULL,
    patient_id UUID,
    encounter_id UUID,
    launch_type VARCHAR(20) NOT NULL,
    launched_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_launch_logs_org ON app_launch_logs(org_id);
CREATE INDEX idx_app_launch_logs_app ON app_launch_logs(app_slug, org_id);

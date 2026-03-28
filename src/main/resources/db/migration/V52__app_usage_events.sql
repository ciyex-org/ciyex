-- Tracks individual app usage events for metering and analytics.
-- Events: app_launch, plugin_render, cds_hook_invocation, api_call, smart_launch
-- Aggregated periodically and reported to marketplace for usage-based billing.

CREATE TABLE app_usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id VARCHAR(100) NOT NULL,
    app_slug VARCHAR(100) NOT NULL,
    app_installation_id UUID REFERENCES app_installations(id),
    event_type VARCHAR(50) NOT NULL,        -- app_launch, plugin_render, cds_hook_invocation, smart_launch, api_call
    event_detail VARCHAR(255),              -- e.g., slot name, hook type, endpoint path
    user_id VARCHAR(255),                   -- user who triggered the event
    patient_id UUID,                        -- if in patient context
    encounter_id UUID,                      -- if in encounter context
    quantity INTEGER NOT NULL DEFAULT 1,    -- for batch events
    recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reported BOOLEAN NOT NULL DEFAULT FALSE -- whether reported to marketplace
);

CREATE INDEX idx_app_usage_events_org ON app_usage_events(org_id, app_slug);
CREATE INDEX idx_app_usage_events_unreported ON app_usage_events(reported) WHERE reported = FALSE;
CREATE INDEX idx_app_usage_events_recorded ON app_usage_events(recorded_at);

-- Aggregated usage summaries per app per org per day (materialized by batch job)
CREATE TABLE app_usage_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id VARCHAR(100) NOT NULL,
    app_slug VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    usage_date DATE NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    unique_users INTEGER NOT NULL DEFAULT 0,
    reported BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, app_slug, event_type, usage_date)
);

CREATE INDEX idx_app_usage_daily_org ON app_usage_daily(org_id, app_slug);
CREATE INDEX idx_app_usage_daily_date ON app_usage_daily(usage_date);

-- =============================================
-- V77: Email & Text (SMS) Notification Integration - Practice Level
-- =============================================

-- ── 1. Practice-level Email/SMS Configuration ──
CREATE TABLE notification_config (
    id              BIGSERIAL PRIMARY KEY,
    channel_type    VARCHAR(20) NOT NULL,           -- email, sms
    provider        VARCHAR(50) NOT NULL,           -- smtp, sendgrid, mailgun, twilio, vonage
    enabled         BOOLEAN NOT NULL DEFAULT false,
    config          JSONB NOT NULL DEFAULT '{}',    -- provider-specific settings (encrypted at app level)
    -- SMTP: {host, port, username, password, from_email, from_name, use_tls}
    -- SendGrid: {api_key, from_email, from_name}
    -- Twilio: {account_sid, auth_token, from_number}
    sender_name     VARCHAR(255),
    sender_address  VARCHAR(255),                   -- email address or phone number
    daily_limit     INTEGER DEFAULT 1000,
    sent_today      INTEGER DEFAULT 0,
    last_reset_date DATE DEFAULT CURRENT_DATE,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(org_alias, channel_type)                 -- one config per channel type per practice
);

CREATE INDEX idx_notification_config_org ON notification_config(org_alias);

-- ── 2. Notification Templates (practice-scoped) ──
CREATE TABLE notification_template (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    template_key    VARCHAR(100) NOT NULL,          -- appointment_reminder, lab_result_ready, prescription_ready, etc.
    channel_type    VARCHAR(20) NOT NULL,           -- email, sms
    subject         VARCHAR(500),                   -- email subject (supports {{variables}})
    body            TEXT NOT NULL,                   -- template body with {{patient_name}}, {{appointment_date}}, etc.
    html_body       TEXT,                           -- HTML version for email
    is_active       BOOLEAN NOT NULL DEFAULT true,
    is_default      BOOLEAN NOT NULL DEFAULT false, -- system default templates
    variables       JSONB DEFAULT '[]',             -- list of available variables for this template
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_template_org ON notification_template(org_alias);
CREATE INDEX idx_notification_template_key ON notification_template(org_alias, template_key, channel_type);

-- ── 3. Notification Log (sent messages history) ──
CREATE TABLE notification_log (
    id              BIGSERIAL PRIMARY KEY,
    channel_type    VARCHAR(20) NOT NULL,           -- email, sms
    recipient       VARCHAR(255) NOT NULL,          -- email address or phone number
    recipient_name  VARCHAR(255),
    template_key    VARCHAR(100),
    subject         VARCHAR(500),
    body            TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'queued',  -- queued, sending, sent, delivered, failed, bounced
    error_message   TEXT,
    external_id     VARCHAR(255),                   -- provider message ID for tracking
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    sent_by         VARCHAR(255),                   -- user who triggered it or 'system' for auto
    trigger_type    VARCHAR(50),                    -- manual, auto_appointment, auto_lab, auto_prescription, bulk
    metadata        JSONB DEFAULT '{}',             -- additional context (appointment_id, order_id, etc.)
    sent_at         TIMESTAMP,
    delivered_at    TIMESTAMP,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_log_org ON notification_log(org_alias, created_at DESC);
CREATE INDEX idx_notification_log_patient ON notification_log(org_alias, patient_id);
CREATE INDEX idx_notification_log_status ON notification_log(org_alias, status);
CREATE INDEX idx_notification_log_channel ON notification_log(org_alias, channel_type, created_at DESC);

-- ── 4. Notification Preferences (which events auto-trigger) ──
CREATE TABLE notification_preference (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,          -- appointment_reminder, appointment_confirmation, lab_result_ready, prescription_ready, recall_due, billing_statement, custom
    email_enabled   BOOLEAN NOT NULL DEFAULT false,
    sms_enabled     BOOLEAN NOT NULL DEFAULT false,
    timing          VARCHAR(50),                    -- 24h_before, 2h_before, immediate, daily_digest, etc.
    template_id     BIGINT REFERENCES notification_template(id),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(org_alias, event_type)
);

CREATE INDEX idx_notification_pref_org ON notification_preference(org_alias);

-- ── 5. Patient Communication Preferences (opt-in/opt-out) ──
CREATE TABLE patient_comm_preference (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(30),
    email_opt_in    BOOLEAN NOT NULL DEFAULT true,
    sms_opt_in      BOOLEAN NOT NULL DEFAULT true,
    preferred_channel VARCHAR(20) DEFAULT 'email',  -- email, sms, both
    language        VARCHAR(10) DEFAULT 'en',
    quiet_hours_start TIME,                         -- e.g., 21:00
    quiet_hours_end   TIME,                         -- e.g., 08:00
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(org_alias, patient_id)
);

CREATE INDEX idx_patient_comm_pref_org ON patient_comm_preference(org_alias);
CREATE INDEX idx_patient_comm_pref_patient ON patient_comm_preference(org_alias, patient_id);

-- ── 6. Bulk Message Campaigns ──
CREATE TABLE bulk_campaign (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    channel_type    VARCHAR(20) NOT NULL,           -- email, sms, both
    template_id     BIGINT REFERENCES notification_template(id),
    subject         VARCHAR(500),
    body            TEXT,
    target_criteria JSONB,                          -- filter criteria: {age_range, gender, diagnosis_codes, last_visit_before, etc.}
    total_recipients INTEGER DEFAULT 0,
    sent_count      INTEGER DEFAULT 0,
    failed_count    INTEGER DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'draft',  -- draft, scheduled, sending, completed, cancelled
    scheduled_at    TIMESTAMP,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_by      VARCHAR(255),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_bulk_campaign_org ON bulk_campaign(org_alias);
CREATE INDEX idx_bulk_campaign_status ON bulk_campaign(org_alias, status);

-- ── 7. Default Templates (system-provided, per org) ──
-- These will be seeded when a new org is created; for existing orgs, insert defaults
-- Using a function to create for all existing orgs

-- Appointment reminder (email)
INSERT INTO notification_template (name, template_key, channel_type, subject, body, html_body, is_active, is_default, variables, org_alias)
SELECT
    'Appointment Reminder',
    'appointment_reminder',
    'email',
    'Reminder: Your appointment on {{appointment_date}}',
    'Hi {{patient_name}},

This is a reminder that you have an appointment scheduled:

Date: {{appointment_date}}
Time: {{appointment_time}}
Provider: {{provider_name}}
Location: {{location_name}}

Please arrive 15 minutes early. If you need to reschedule, please call us at {{practice_phone}}.

Thank you,
{{practice_name}}',
    NULL,
    true,
    true,
    '["patient_name", "appointment_date", "appointment_time", "provider_name", "location_name", "practice_phone", "practice_name"]'::jsonb,
    oa.org_alias
FROM (SELECT DISTINCT org_alias FROM channel WHERE org_alias IS NOT NULL) oa
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template nt
    WHERE nt.org_alias = oa.org_alias AND nt.template_key = 'appointment_reminder' AND nt.channel_type = 'email'
);

-- Appointment reminder (SMS)
INSERT INTO notification_template (name, template_key, channel_type, subject, body, is_active, is_default, variables, org_alias)
SELECT
    'Appointment Reminder (SMS)',
    'appointment_reminder',
    'sms',
    NULL,
    'Hi {{patient_name}}, reminder: Appt on {{appointment_date}} at {{appointment_time}} with {{provider_name}}. Reply STOP to opt out.',
    true,
    true,
    '["patient_name", "appointment_date", "appointment_time", "provider_name"]'::jsonb,
    oa.org_alias
FROM (SELECT DISTINCT org_alias FROM channel WHERE org_alias IS NOT NULL) oa
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template nt
    WHERE nt.org_alias = oa.org_alias AND nt.template_key = 'appointment_reminder' AND nt.channel_type = 'sms'
);

-- Lab result ready (email)
INSERT INTO notification_template (name, template_key, channel_type, subject, body, is_active, is_default, variables, org_alias)
SELECT
    'Lab Results Available',
    'lab_result_ready',
    'email',
    'Your lab results are available',
    'Hi {{patient_name}},

Your lab results from {{order_date}} are now available. Please log in to the patient portal to view your results, or contact our office for more information.

{{practice_name}}
{{practice_phone}}',
    true,
    true,
    '["patient_name", "order_date", "practice_name", "practice_phone"]'::jsonb,
    oa.org_alias
FROM (SELECT DISTINCT org_alias FROM channel WHERE org_alias IS NOT NULL) oa
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template nt
    WHERE nt.org_alias = oa.org_alias AND nt.template_key = 'lab_result_ready' AND nt.channel_type = 'email'
);

-- Menu item for notifications settings
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'notifications', 'Notifications', 'Bell', '/notifications', 11);

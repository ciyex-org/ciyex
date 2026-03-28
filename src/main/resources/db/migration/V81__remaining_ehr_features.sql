-- =============================================
-- V80: Remaining EHR Features - Portal, CDS, Growth Charts, Kiosk
-- =============================================

-- ── 1. Patient Portal Configuration ──
CREATE TABLE IF NOT EXISTS portal_config (
    id              BIGSERIAL PRIMARY KEY,
    enabled         BOOLEAN NOT NULL DEFAULT false,
    portal_url      VARCHAR(500),
    features        JSONB NOT NULL DEFAULT '{}',    -- {appointment_requests, secure_messaging, lab_results, medication_refills, bill_pay, demographics_update}
    branding        JSONB NOT NULL DEFAULT '{}',    -- {logo_url, primary_color, practice_name, welcome_message}
    registration    JSONB NOT NULL DEFAULT '{}',    -- {self_registration, require_verification, allowed_domains}
    org_alias       VARCHAR(100) NOT NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_portal_config_org ON portal_config(org_alias);

-- ── 2. Portal Access Requests (patients requesting portal access) ──
CREATE TABLE portal_access_request (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    date_of_birth   DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',  -- pending, approved, denied
    approved_by     VARCHAR(255),
    approved_at     TIMESTAMP,
    denied_reason   TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_portal_access_org ON portal_access_request(org_alias);
CREATE INDEX idx_portal_access_status ON portal_access_request(org_alias, status);

-- ── 3. Clinical Decision Support Rules ──
CREATE TABLE cds_rule (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    rule_type       VARCHAR(50) NOT NULL,          -- preventive_screening, drug_allergy, drug_drug, duplicate_order, age_based, condition_based, lab_value, custom
    category        VARCHAR(100),                   -- preventive, medication_safety, order_entry, chronic_disease
    trigger_event   VARCHAR(100),                  -- encounter_open, order_entry, medication_prescribe, lab_result, manual
    conditions      JSONB NOT NULL DEFAULT '{}',   -- {age_min, age_max, gender, diagnosis_codes[], medication_codes[], lab_codes[]}
    action_type     VARCHAR(50) NOT NULL DEFAULT 'alert',  -- alert, reminder, suggestion, hard_stop
    severity        VARCHAR(20) NOT NULL DEFAULT 'info',   -- info, warning, critical
    message         TEXT NOT NULL,                  -- alert message shown to provider
    recommendation  TEXT,                          -- recommended action
    reference_url   VARCHAR(1000),                -- clinical guideline reference
    is_active       BOOLEAN NOT NULL DEFAULT true,
    applies_to      VARCHAR(50) DEFAULT 'all',    -- all, provider, nurse, ma
    snooze_days     INTEGER DEFAULT 0,            -- 0 = no snooze, >0 = days before re-alerting
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cds_rule_org ON cds_rule(org_alias);
CREATE INDEX idx_cds_rule_type ON cds_rule(org_alias, rule_type);
CREATE INDEX idx_cds_rule_trigger ON cds_rule(org_alias, trigger_event);
CREATE INDEX idx_cds_rule_active ON cds_rule(org_alias, is_active) WHERE is_active = true;

-- ── 4. CDS Alert Log (fired alerts history) ──
CREATE TABLE cds_alert_log (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT REFERENCES cds_rule(id),
    rule_name       VARCHAR(255),
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    encounter_id    BIGINT,
    alert_type      VARCHAR(50),
    severity        VARCHAR(20),
    message         TEXT,
    action_taken    VARCHAR(50),                   -- acknowledged, overridden, acted_on, snoozed, dismissed
    override_reason TEXT,
    acted_by        VARCHAR(255),
    acted_at        TIMESTAMP,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cds_alert_org ON cds_alert_log(org_alias, created_at DESC);
CREATE INDEX idx_cds_alert_patient ON cds_alert_log(org_alias, patient_id);
CREATE INDEX idx_cds_alert_rule ON cds_alert_log(rule_id);

-- ── 5. Growth Chart Measurements (pediatric) ──
-- Uses vitals data but stored separately for growth tracking
CREATE TABLE growth_measurement (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    measurement_date DATE NOT NULL,
    age_months      DECIMAL(6,2),                  -- calculated from DOB
    gender          VARCHAR(10),                   -- male, female
    weight_kg       DECIMAL(6,2),
    height_cm       DECIMAL(6,2),
    bmi             DECIMAL(5,2),                  -- calculated
    head_circ_cm    DECIMAL(5,2),                  -- head circumference
    weight_percentile DECIMAL(5,2),
    height_percentile DECIMAL(5,2),
    bmi_percentile  DECIMAL(5,2),
    head_circ_percentile DECIMAL(5,2),
    chart_standard  VARCHAR(10) DEFAULT 'WHO',     -- WHO (0-2yr), CDC (2-20yr)
    encounter_id    BIGINT,
    measured_by     VARCHAR(255),
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_growth_org ON growth_measurement(org_alias);
CREATE INDEX idx_growth_patient ON growth_measurement(org_alias, patient_id, measurement_date DESC);

-- ── 6. Kiosk Configuration ──
CREATE TABLE kiosk_config (
    id              BIGSERIAL PRIMARY KEY,
    enabled         BOOLEAN NOT NULL DEFAULT false,
    config          JSONB NOT NULL DEFAULT '{}',   -- {verify_dob, verify_phone, update_demographics, update_insurance, sign_consent, collect_copay, show_wait_time}
    welcome_message TEXT DEFAULT 'Welcome! Please check in for your appointment.',
    completion_message TEXT DEFAULT 'Thank you! Please have a seat and we will call you shortly.',
    idle_timeout_sec INTEGER DEFAULT 120,
    org_alias       VARCHAR(100) NOT NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_kiosk_config_org ON kiosk_config(org_alias);

-- ── 7. Kiosk Check-in Log ──
CREATE TABLE kiosk_checkin (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    appointment_id  BIGINT,
    check_in_time   TIMESTAMP NOT NULL DEFAULT now(),
    demographics_updated BOOLEAN DEFAULT false,
    insurance_updated    BOOLEAN DEFAULT false,
    consent_signed       BOOLEAN DEFAULT false,
    copay_collected      BOOLEAN DEFAULT false,
    copay_amount         DECIMAL(10,2),
    verification_method  VARCHAR(50),              -- dob, phone, both
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_kiosk_checkin_org ON kiosk_checkin(org_alias, created_at DESC);
CREATE INDEX idx_kiosk_checkin_patient ON kiosk_checkin(org_alias, patient_id);

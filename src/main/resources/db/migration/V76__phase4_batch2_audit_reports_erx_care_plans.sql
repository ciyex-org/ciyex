-- =============================================
-- V76: Phase 4 Batch 2 - Audit Trail, Reports, E-Prescribing, Care Plans,
--      Consent Management, Immunizations, Fax Queue
-- =============================================

-- ── 1. Audit Trail / Activity Log ──
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(50) NOT NULL,          -- VIEW, CREATE, UPDATE, DELETE, SIGN, PRINT, EXPORT
    resource_type   VARCHAR(100) NOT NULL,         -- Patient, Encounter, LabOrder, Prescription, etc.
    resource_id     VARCHAR(100),
    resource_name   VARCHAR(500),
    user_id         VARCHAR(255) NOT NULL,
    user_name       VARCHAR(255),
    user_role       VARCHAR(100),
    ip_address      VARCHAR(50),
    details         JSONB,                         -- changed fields, old/new values
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_org ON audit_log(org_alias);
CREATE INDEX idx_audit_log_user ON audit_log(org_alias, user_id, created_at DESC);
CREATE INDEX idx_audit_log_patient ON audit_log(org_alias, patient_id, created_at DESC);
CREATE INDEX idx_audit_log_resource ON audit_log(org_alias, resource_type, resource_id);
CREATE INDEX idx_audit_log_action ON audit_log(org_alias, action, created_at DESC);
CREATE INDEX idx_audit_log_date ON audit_log(org_alias, created_at DESC);

-- ── 2. Saved Reports ──
CREATE TABLE saved_report (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    report_type     VARCHAR(50) NOT NULL,          -- demographics, visits, revenue, appointments, encounters, custom
    description     TEXT,
    query_config    JSONB NOT NULL,                 -- filters, date range, grouping, columns
    schedule        VARCHAR(50),                    -- daily, weekly, monthly, none
    schedule_email  VARCHAR(255),
    last_run_at     TIMESTAMP,
    created_by      VARCHAR(255),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_saved_report_org ON saved_report(org_alias);
CREATE INDEX idx_saved_report_type ON saved_report(org_alias, report_type);

-- ── 3. Prescriptions (E-Prescribing) ──
CREATE TABLE prescription (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    encounter_id        BIGINT,
    prescriber_name     VARCHAR(255),
    prescriber_npi      VARCHAR(20),
    medication_name     VARCHAR(500) NOT NULL,
    medication_code     VARCHAR(50),                -- NDC or RxNorm code
    medication_system   VARCHAR(20) DEFAULT 'NDC',  -- NDC, RxNorm
    strength            VARCHAR(100),
    dosage_form         VARCHAR(100),               -- tablet, capsule, solution, injection, etc.
    sig                 TEXT NOT NULL,               -- SIG directions (e.g., "Take 1 tablet by mouth twice daily")
    quantity            INTEGER,
    quantity_unit       VARCHAR(50),
    days_supply         INTEGER,
    refills             INTEGER DEFAULT 0,
    refills_remaining   INTEGER DEFAULT 0,
    pharmacy_name       VARCHAR(255),
    pharmacy_phone      VARCHAR(30),
    pharmacy_address    TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'active',   -- active, completed, cancelled, on_hold, discontinued
    priority            VARCHAR(20) DEFAULT 'routine',            -- routine, urgent, stat
    start_date          DATE,
    end_date            DATE,
    discontinued_date   DATE,
    discontinued_reason TEXT,
    notes               TEXT,
    dea_schedule        VARCHAR(10),                -- II, III, IV, V (controlled substance schedule)
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_prescription_org ON prescription(org_alias);
CREATE INDEX idx_prescription_patient ON prescription(org_alias, patient_id);
CREATE INDEX idx_prescription_status ON prescription(org_alias, status);
CREATE INDEX idx_prescription_encounter ON prescription(org_alias, encounter_id);

-- ── 4. Drug Interactions (reference data) ──
CREATE TABLE drug_interaction (
    id              BIGSERIAL PRIMARY KEY,
    drug_a_code     VARCHAR(50) NOT NULL,
    drug_a_name     VARCHAR(500) NOT NULL,
    drug_b_code     VARCHAR(50) NOT NULL,
    drug_b_name     VARCHAR(500) NOT NULL,
    severity        VARCHAR(20) NOT NULL,          -- minor, moderate, major, contraindicated
    description     TEXT NOT NULL,
    clinical_effect TEXT,
    management      TEXT,
    source          VARCHAR(100) DEFAULT 'internal',
    org_alias       VARCHAR(100) NOT NULL DEFAULT '__GLOBAL__',
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_drug_interaction_a ON drug_interaction(drug_a_code);
CREATE INDEX idx_drug_interaction_b ON drug_interaction(drug_b_code);
CREATE INDEX idx_drug_interaction_severity ON drug_interaction(severity);

-- ── 5. Care Plans ──
CREATE TABLE care_plan (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    title           VARCHAR(500) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'active',   -- draft, active, completed, revoked, on_hold
    category        VARCHAR(100),                             -- chronic_disease, preventive, post_surgical, behavioral, etc.
    start_date      DATE,
    end_date        DATE,
    author_name     VARCHAR(255),
    description     TEXT,
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_care_plan_org ON care_plan(org_alias);
CREATE INDEX idx_care_plan_patient ON care_plan(org_alias, patient_id);
CREATE INDEX idx_care_plan_status ON care_plan(org_alias, status);

-- Care Plan Goals
CREATE TABLE care_plan_goal (
    id              BIGSERIAL PRIMARY KEY,
    care_plan_id    BIGINT NOT NULL REFERENCES care_plan(id) ON DELETE CASCADE,
    description     TEXT NOT NULL,
    target_date     DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'in_progress',  -- in_progress, achieved, not_achieved, cancelled
    measure         VARCHAR(255),                                 -- e.g., "HbA1c < 7%", "BMI < 25"
    current_value   VARCHAR(100),
    target_value    VARCHAR(100),
    priority        VARCHAR(20) DEFAULT 'medium',
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_care_plan_goal_plan ON care_plan_goal(care_plan_id);

-- Care Plan Interventions
CREATE TABLE care_plan_intervention (
    id              BIGSERIAL PRIMARY KEY,
    care_plan_id    BIGINT NOT NULL REFERENCES care_plan(id) ON DELETE CASCADE,
    goal_id         BIGINT REFERENCES care_plan_goal(id) ON DELETE SET NULL,
    description     TEXT NOT NULL,
    assigned_to     VARCHAR(255),
    frequency       VARCHAR(100),                  -- daily, weekly, monthly, as_needed, once
    status          VARCHAR(30) NOT NULL DEFAULT 'active',   -- active, completed, cancelled
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_care_plan_intervention_plan ON care_plan_intervention(care_plan_id);

-- ── 6. Patient Consent Management ──
CREATE TABLE patient_consent (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    consent_type    VARCHAR(100) NOT NULL,         -- hipaa_privacy, treatment, release_of_info, telehealth, research, financial
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',  -- pending, signed, expired, revoked
    signed_date     DATE,
    expiry_date     DATE,
    signed_by       VARCHAR(255),                  -- patient or guardian name
    witness_name    VARCHAR(255),
    document_url    VARCHAR(1000),                 -- link to signed PDF/file
    version         VARCHAR(20),                   -- consent form version
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_patient_consent_org ON patient_consent(org_alias);
CREATE INDEX idx_patient_consent_patient ON patient_consent(org_alias, patient_id);
CREATE INDEX idx_patient_consent_type ON patient_consent(org_alias, consent_type);
CREATE INDEX idx_patient_consent_status ON patient_consent(org_alias, status);

-- ── 7. Immunization Records ──
CREATE TABLE immunization_record (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    vaccine_name        VARCHAR(500) NOT NULL,
    cvx_code            VARCHAR(20),               -- CVX code from ciyex-codes
    lot_number          VARCHAR(50),
    manufacturer        VARCHAR(255),
    administration_date DATE NOT NULL,
    expiration_date     DATE,
    site                VARCHAR(100),              -- left arm, right arm, left thigh, etc.
    route               VARCHAR(50),               -- intramuscular, subcutaneous, oral, intranasal
    dose_number         INTEGER,
    dose_series         VARCHAR(50),               -- e.g., "1 of 3", "booster"
    administered_by     VARCHAR(255),
    ordering_provider   VARCHAR(255),
    status              VARCHAR(30) NOT NULL DEFAULT 'completed',  -- completed, entered_in_error, not_done
    refusal_reason      TEXT,
    reaction            TEXT,
    vis_date            DATE,                      -- Vaccine Information Statement date given
    notes               TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_immunization_org ON immunization_record(org_alias);
CREATE INDEX idx_immunization_patient ON immunization_record(org_alias, patient_id);
CREATE INDEX idx_immunization_cvx ON immunization_record(cvx_code);

-- ── 8. Fax Queue ──
CREATE TABLE fax_message (
    id              BIGSERIAL PRIMARY KEY,
    direction       VARCHAR(10) NOT NULL,          -- inbound, outbound
    fax_number      VARCHAR(30),
    sender_name     VARCHAR(255),
    recipient_name  VARCHAR(255),
    subject         VARCHAR(500),
    page_count      INTEGER,
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',  -- pending, sending, sent, delivered, failed, received, categorized, attached
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    category        VARCHAR(100),                  -- referral, lab_result, prior_auth, medical_records, other
    document_url    VARCHAR(1000),
    error_message   TEXT,
    sent_at         TIMESTAMP,
    received_at     TIMESTAMP,
    processed_by    VARCHAR(255),
    processed_at    TIMESTAMP,
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_fax_org ON fax_message(org_alias);
CREATE INDEX idx_fax_direction ON fax_message(org_alias, direction, status);
CREATE INDEX idx_fax_patient ON fax_message(org_alias, patient_id);
CREATE INDEX idx_fax_status ON fax_message(org_alias, status);

-- ── 9. Menu Items for New Pages ──
-- Audit Log under admin
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'audit-log', 'Audit Log', 'ScrollText', '/admin/audit-log', 14);

-- Reports
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'reports', 'Reports', 'BarChart3', '/reports', 9);

-- Fax
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'fax', 'Fax', 'Printer', '/fax', 10);

-- Portal Settings (under settings group)
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'kiosk', 'Check-in Kiosk', 'TabletSmartphone', '/kiosk', 15);

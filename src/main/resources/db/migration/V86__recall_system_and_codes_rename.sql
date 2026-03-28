-- V86: Industry-standard Recall system + Codes menu rename
-- Recall is operational data → PostgreSQL, NOT FHIR

-- ═══════════════════════════════════════════════════════
-- 1. RECALL TYPE (configurable per org)
-- ═══════════════════════════════════════════════════════
CREATE TABLE recall_type (
    id              BIGSERIAL PRIMARY KEY,
    org_alias       VARCHAR(100) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    category        VARCHAR(30) NOT NULL DEFAULT 'PREVENTIVE',
    interval_months INT NOT NULL DEFAULT 12,
    interval_days   INT DEFAULT 0,
    lead_time_days  INT NOT NULL DEFAULT 30,
    max_attempts    INT NOT NULL DEFAULT 4,
    priority        VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    auto_create     BOOLEAN NOT NULL DEFAULT false,
    communication_sequence JSONB DEFAULT '["PORTAL","SMS","PHONE","LETTER"]'::jsonb,
    escalation_wait_days   JSONB DEFAULT '[7,7,14]'::jsonb,
    appointment_type_code  VARCHAR(50),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    created_by      VARCHAR(100),
    UNIQUE (org_alias, code)
);

ALTER TABLE recall_type ENABLE ROW LEVEL SECURITY;
CREATE POLICY recall_type_rls ON recall_type
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_recall_type_org ON recall_type(org_alias);

-- ═══════════════════════════════════════════════════════
-- 2. PATIENT RECALL (core record)
-- ═══════════════════════════════════════════════════════
CREATE TABLE patient_recall (
    id                     BIGSERIAL PRIMARY KEY,
    org_alias              VARCHAR(100) NOT NULL,
    patient_id             BIGINT NOT NULL,
    patient_name           VARCHAR(200),
    patient_phone          VARCHAR(30),
    patient_email          VARCHAR(200),
    recall_type_id         BIGINT REFERENCES recall_type(id),
    recall_type_name       VARCHAR(100),
    provider_id            BIGINT,
    provider_name          VARCHAR(200),
    location_id            BIGINT,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date               DATE NOT NULL,
    notification_date      DATE,
    source_encounter_id    VARCHAR(100),
    source_appointment_id  BIGINT,
    linked_appointment_id  BIGINT,
    completed_encounter_id VARCHAR(100),
    completed_date         DATE,
    attempt_count          INT NOT NULL DEFAULT 0,
    last_attempt_date      TIMESTAMP,
    last_attempt_method    VARCHAR(20),
    last_attempt_outcome   VARCHAR(30),
    next_attempt_date      DATE,
    preferred_contact      VARCHAR(20) DEFAULT 'PHONE',
    priority               VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    notes                  TEXT,
    cancelled_reason       TEXT,
    auto_created           BOOLEAN NOT NULL DEFAULT false,
    created_at             TIMESTAMP DEFAULT now(),
    updated_at             TIMESTAMP DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);

ALTER TABLE patient_recall ENABLE ROW LEVEL SECURITY;
CREATE POLICY patient_recall_rls ON patient_recall
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_patient_recall_org ON patient_recall(org_alias);
CREATE INDEX idx_patient_recall_patient ON patient_recall(patient_id);
CREATE INDEX idx_patient_recall_status ON patient_recall(org_alias, status);
CREATE INDEX idx_patient_recall_due ON patient_recall(org_alias, due_date);
CREATE INDEX idx_patient_recall_provider ON patient_recall(org_alias, provider_id);
CREATE INDEX idx_patient_recall_type ON patient_recall(recall_type_id);

-- ═══════════════════════════════════════════════════════
-- 3. RECALL OUTREACH LOG (contact attempts)
-- ═══════════════════════════════════════════════════════
CREATE TABLE recall_outreach_log (
    id                  BIGSERIAL PRIMARY KEY,
    recall_id           BIGINT NOT NULL REFERENCES patient_recall(id) ON DELETE CASCADE,
    org_alias           VARCHAR(100) NOT NULL,
    attempt_number      INT NOT NULL,
    attempt_date        TIMESTAMP NOT NULL DEFAULT now(),
    method              VARCHAR(20) NOT NULL,
    direction           VARCHAR(10) NOT NULL DEFAULT 'OUTBOUND',
    performed_by        VARCHAR(100),
    performed_by_name   VARCHAR(200),
    outcome             VARCHAR(30) NOT NULL,
    notes               TEXT,
    next_action         VARCHAR(20),
    next_action_date    DATE,
    automated           BOOLEAN NOT NULL DEFAULT false,
    delivery_status     VARCHAR(20),
    created_at          TIMESTAMP DEFAULT now()
);

ALTER TABLE recall_outreach_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY recall_outreach_rls ON recall_outreach_log
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_recall_outreach_recall ON recall_outreach_log(recall_id);
CREATE INDEX idx_recall_outreach_org ON recall_outreach_log(org_alias);

-- ═══════════════════════════════════════════════════════
-- 4. SEED DEFAULT RECALL TYPES (template for all orgs)
-- ═══════════════════════════════════════════════════════
INSERT INTO recall_type (org_alias, name, code, category, interval_months, lead_time_days, max_attempts, priority, auto_create) VALUES
('__SYSTEM__', 'Annual Physical',         'annual-physical',    'PREVENTIVE',    12, 30, 4, 'NORMAL', true),
('__SYSTEM__', 'Well-Child Visit',        'well-child',         'PREVENTIVE',     12, 30, 4, 'NORMAL', true),
('__SYSTEM__', 'Medicare Wellness',        'medicare-wellness',  'PREVENTIVE',     12, 30, 4, 'NORMAL', true),
('__SYSTEM__', 'Dental Cleaning',          'dental-cleaning',    'DENTAL',          6, 21, 4, 'NORMAL', true),
('__SYSTEM__', 'Periodontal Maintenance',  'perio-maintenance',  'DENTAL',          3, 21, 4, 'HIGH',   true),
('__SYSTEM__', 'Diabetes Follow-Up',       'diabetes-followup',  'CHRONIC',         3, 14, 4, 'HIGH',   true),
('__SYSTEM__', 'Hypertension Check',       'htn-check',          'CHRONIC',         3, 14, 4, 'HIGH',   true),
('__SYSTEM__', 'Asthma Review',            'asthma-review',      'CHRONIC',         6, 21, 3, 'NORMAL', false),
('__SYSTEM__', 'Flu Shot',                 'flu-shot',           'IMMUNIZATION',   12, 60, 3, 'NORMAL', false),
('__SYSTEM__', 'COVID Booster',            'covid-booster',      'IMMUNIZATION',   12, 30, 3, 'NORMAL', false),
('__SYSTEM__', 'Mammogram',                'mammogram',          'SCREENING',      12, 30, 3, 'NORMAL', false),
('__SYSTEM__', 'Colonoscopy',              'colonoscopy',        'SCREENING',     120, 90, 4, 'NORMAL', false),
('__SYSTEM__', 'Pap Smear',               'pap-smear',          'SCREENING',      36, 30, 3, 'NORMAL', false),
('__SYSTEM__', 'A1C Lab',                  'a1c-lab',            'LAB',             3, 14, 3, 'NORMAL', false),
('__SYSTEM__', 'Lipid Panel',              'lipid-panel',        'LAB',            12, 30, 3, 'NORMAL', false),
('__SYSTEM__', 'Post-Surgical Follow-Up',  'post-surgical',      'POST_PROCEDURE',  0, 7, 3, 'HIGH',   false),
('__SYSTEM__', 'Eye Exam (Diabetic)',      'diabetic-eye-exam',  'SPECIALIST',     12, 30, 3, 'NORMAL', false),
('__SYSTEM__', 'Cardiology Follow-Up',     'cardio-followup',    'SPECIALIST',      6, 21, 3, 'NORMAL', false);

-- ═══════════════════════════════════════════════════════
-- 5. RENAME "Codes List" → "Codes" + point to /codes
-- ═══════════════════════════════════════════════════════
UPDATE menu_item
SET label = 'Codes', screen_slug = '/codes'
WHERE item_key = 'codes-list' OR (label = 'Codes List');

-- Also update the recall menu to point to /recall (ensure it exists)
UPDATE menu_item
SET screen_slug = '/recall'
WHERE item_key = 'recall' AND (screen_slug IS NULL OR screen_slug = '');

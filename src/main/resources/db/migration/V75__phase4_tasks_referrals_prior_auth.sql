-- =============================================
-- V75: Phase 4 - Task Management, Referral Management, Prior Authorization
-- =============================================

-- ── 1. Clinical Tasks ──
CREATE TABLE clinical_task (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    task_type       VARCHAR(50) NOT NULL DEFAULT 'general',   -- general, follow_up, callback, refill, lab_review, referral, prior_auth, documentation
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',   -- pending, in_progress, completed, cancelled, deferred
    priority        VARCHAR(20) NOT NULL DEFAULT 'normal',    -- low, normal, high, urgent
    due_date        DATE,
    due_time        TIME,
    assigned_to     VARCHAR(255),
    assigned_by     VARCHAR(255),
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    encounter_id    BIGINT,
    reference_type  VARCHAR(50),    -- lab_order, referral, prior_auth, prescription, etc.
    reference_id    BIGINT,
    completed_at    TIMESTAMP,
    completed_by    VARCHAR(255),
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_clinical_task_org ON clinical_task(org_alias);
CREATE INDEX idx_clinical_task_assigned ON clinical_task(org_alias, assigned_to, status);
CREATE INDEX idx_clinical_task_patient ON clinical_task(org_alias, patient_id);
CREATE INDEX idx_clinical_task_status ON clinical_task(org_alias, status, due_date);
CREATE INDEX idx_clinical_task_type ON clinical_task(org_alias, task_type);

-- ── 2. Referrals ──
CREATE TABLE referral (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    referring_provider  VARCHAR(255),
    specialist_name     VARCHAR(255),
    specialist_npi      VARCHAR(20),
    specialty           VARCHAR(255),
    facility_name       VARCHAR(255),
    facility_address    TEXT,
    facility_phone      VARCHAR(30),
    facility_fax        VARCHAR(30),
    reason              TEXT NOT NULL,
    clinical_notes      TEXT,
    urgency             VARCHAR(20) NOT NULL DEFAULT 'routine',  -- routine, urgent, stat
    status              VARCHAR(30) NOT NULL DEFAULT 'draft',    -- draft, sent, acknowledged, scheduled, completed, cancelled, denied
    referral_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date         DATE,
    authorization_number VARCHAR(50),
    insurance_name      VARCHAR(255),
    insurance_id        VARCHAR(50),
    appointment_date    DATE,
    appointment_notes   TEXT,
    follow_up_notes     TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_referral_org ON referral(org_alias);
CREATE INDEX idx_referral_patient ON referral(org_alias, patient_id);
CREATE INDEX idx_referral_status ON referral(org_alias, status);

-- ── 3. Prior Authorizations ──
CREATE TABLE prior_authorization (
    id                      BIGSERIAL PRIMARY KEY,
    patient_id              BIGINT NOT NULL,
    patient_name            VARCHAR(255),
    provider_name           VARCHAR(255),
    insurance_name          VARCHAR(255),
    insurance_id            VARCHAR(50),
    member_id               VARCHAR(50),
    auth_number             VARCHAR(50),
    procedure_code          VARCHAR(20),
    procedure_description   VARCHAR(500),
    diagnosis_code          VARCHAR(20),
    diagnosis_description   VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'pending',  -- pending, submitted, approved, denied, appeal, expired, cancelled
    priority                VARCHAR(20) NOT NULL DEFAULT 'routine',  -- routine, urgent, stat
    requested_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    review_date             DATE,
    approved_date           DATE,
    denied_date             DATE,
    expiry_date             DATE,
    approved_units          INTEGER,
    used_units              INTEGER DEFAULT 0,
    remaining_units         INTEGER,
    denial_reason           TEXT,
    appeal_deadline         DATE,
    notes                   TEXT,
    org_alias               VARCHAR(100) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_prior_auth_org ON prior_authorization(org_alias);
CREATE INDEX idx_prior_auth_patient ON prior_authorization(org_alias, patient_id);
CREATE INDEX idx_prior_auth_status ON prior_authorization(org_alias, status);

-- ── 4. Menu Items for EHR Sidebar ──
-- Add Tasks between Encounters and Messaging
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'tasks', 'Tasks', 'CheckSquare', '/tasks', 4);

-- Add Referrals after Claim Management
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'referrals', 'Referrals', 'ArrowRightLeft', '/referrals', 7);

-- Add Prior Auth after Referrals
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'prior-auth', 'Authorizations', 'ShieldCheck', '/authorizations', 8);

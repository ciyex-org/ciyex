-- =============================================
-- V80: Patient Consent Management & Immunization Registry
-- =============================================

-- ── 1. Patient Consent ──
CREATE TABLE IF NOT EXISTS patient_consent (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    consent_type    VARCHAR(50) NOT NULL DEFAULT 'hipaa_privacy',  -- hipaa_privacy, treatment, release_of_info, telehealth, research, financial
    status          VARCHAR(30) NOT NULL DEFAULT 'pending',         -- pending, signed, expired, revoked
    signed_date     DATE,
    expiry_date     DATE,
    signed_by       VARCHAR(255),
    witness_name    VARCHAR(255),
    document_url    TEXT,
    version         VARCHAR(20),
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_patient_consent_org ON patient_consent(org_alias);
CREATE INDEX IF NOT EXISTS idx_patient_consent_patient ON patient_consent(org_alias, patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_consent_status ON patient_consent(org_alias, status);
CREATE INDEX IF NOT EXISTS idx_patient_consent_type ON patient_consent(org_alias, consent_type);

-- ── 2. Immunization Record ──
CREATE TABLE IF NOT EXISTS immunization_record (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT,
    patient_name        VARCHAR(255),
    vaccine_name        VARCHAR(255),
    cvx_code            VARCHAR(20),
    lot_number          VARCHAR(50),
    manufacturer        VARCHAR(255),
    administration_date DATE,
    expiration_date     DATE,
    site                VARCHAR(50),       -- left arm, right arm, left thigh, etc.
    route               VARCHAR(50),       -- intramuscular, subcutaneous, oral, intranasal
    dose_number         INTEGER,
    dose_series         VARCHAR(50),       -- e.g., "1 of 3", "booster"
    administered_by     VARCHAR(255),
    ordering_provider   VARCHAR(255),
    status              VARCHAR(30) NOT NULL DEFAULT 'completed',  -- completed, entered_in_error, not_done
    refusal_reason      TEXT,
    reaction            TEXT,
    vis_date            DATE,              -- Vaccine Information Statement date given
    notes               TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_immunization_org ON immunization_record(org_alias);
CREATE INDEX IF NOT EXISTS idx_immunization_patient ON immunization_record(org_alias, patient_id);
CREATE INDEX IF NOT EXISTS idx_immunization_cvx ON immunization_record(org_alias, cvx_code);
CREATE INDEX IF NOT EXISTS idx_immunization_status ON immunization_record(org_alias, status);
CREATE INDEX IF NOT EXISTS idx_immunization_admin_date ON immunization_record(org_alias, administration_date);

-- ── 3. Menu Items for EHR Sidebar ──
-- Add Consents menu item
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'consents', 'Consents', 'FileCheck', '/consents', 9
WHERE NOT EXISTS (
    SELECT 1 FROM menu_item WHERE item_key = 'consents' AND menu_id = 'a0000000-0000-0000-0000-000000000001'
);

-- Add Immunizations menu item
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'immunizations', 'Immunizations', 'Syringe', '/immunizations', 10
WHERE NOT EXISTS (
    SELECT 1 FROM menu_item WHERE item_key = 'immunizations' AND menu_id = 'a0000000-0000-0000-0000-000000000001'
);

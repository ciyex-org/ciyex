-- =============================================
-- V82: Menu items for CDS, Prescriptions, Care Plans, Document Scanning
--      + scanned_document table
-- =============================================

-- ── 1. Scanned Document table (Document Scanning / OCR) ──
CREATE TABLE scanned_document (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    mime_type       VARCHAR(100),
    category        VARCHAR(50) DEFAULT 'other',  -- insurance_card, lab_report, referral, consent, id_document, rx, other
    patient_id      BIGINT,
    patient_name    VARCHAR(255),
    encounter_id    BIGINT,
    storage_path    TEXT,                          -- S3 path or local path
    ocr_status      VARCHAR(30) DEFAULT 'pending', -- pending, processing, completed, failed
    ocr_text        TEXT,                          -- extracted text
    ocr_confidence  DECIMAL(5,2),                  -- 0-100
    ocr_metadata    JSONB DEFAULT '{}',            -- structured extraction results
    uploaded_by     VARCHAR(255),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_scanned_doc_org ON scanned_document(org_alias);
CREATE INDEX idx_scanned_doc_patient ON scanned_document(org_alias, patient_id);
CREATE INDEX idx_scanned_doc_status ON scanned_document(org_alias, ocr_status);
CREATE INDEX idx_scanned_doc_category ON scanned_document(org_alias, category);

-- ── 2. Menu Items for new pages ──

-- Clinical Decision Support
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'cds', 'Clinical Alerts', 'ShieldAlert', '/cds', 13
WHERE NOT EXISTS (SELECT 1 FROM menu_item WHERE item_key = 'cds' AND menu_id = 'a0000000-0000-0000-0000-000000000001');

-- E-Prescribing
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'prescriptions', 'Prescriptions', 'Pill', '/prescriptions', 5
WHERE NOT EXISTS (SELECT 1 FROM menu_item WHERE item_key = 'prescriptions' AND menu_id = 'a0000000-0000-0000-0000-000000000001');

-- Care Plans
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'care-plans', 'Care Plans', 'HeartPulse', '/care-plans', 6
WHERE NOT EXISTS (SELECT 1 FROM menu_item WHERE item_key = 'care-plans' AND menu_id = 'a0000000-0000-0000-0000-000000000001');

-- Document Scanning / OCR
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
SELECT 'a0000000-0000-0000-0000-000000000001', 'document-scanning', 'Doc Scanning', 'ScanLine', '/document-scanning', 16
WHERE NOT EXISTS (SELECT 1 FROM menu_item WHERE item_key = 'document-scanning' AND menu_id = 'a0000000-0000-0000-0000-000000000001');

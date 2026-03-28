CREATE TABLE IF NOT EXISTS scanned_documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(512) NOT NULL,
    original_file_name VARCHAR(512),
    file_size BIGINT,
    mime_type VARCHAR(128),
    file_url TEXT,
    storage_key TEXT,
    patient_id BIGINT,
    patient_name VARCHAR(255),
    category VARCHAR(64),
    document_date DATE,
    ocr_text TEXT,
    ocr_status VARCHAR(32) DEFAULT 'pending',
    ocr_confidence DOUBLE PRECISION,
    tags TEXT,
    notes TEXT,
    uploaded_by VARCHAR(255),
    org_alias VARCHAR(128),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scanned_documents_org_alias ON scanned_documents(org_alias);
CREATE INDEX IF NOT EXISTS idx_scanned_documents_patient_id ON scanned_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_scanned_documents_ocr_status ON scanned_documents(ocr_status);

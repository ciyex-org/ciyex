-- =============================================
-- V79: Patient Education - Materials & Assignments
-- =============================================

-- ── 1. Education Material Library (practice-scoped catalog) ──
CREATE TABLE education_material (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    category        VARCHAR(100),               -- diabetes, hypertension, post_surgical, prenatal, medication, nutrition, exercise, mental_health, preventive, pediatric, other
    content_type    VARCHAR(50) NOT NULL DEFAULT 'article',  -- article, video, pdf, link, handout, infographic
    content         TEXT,                        -- rich text body (for articles/handouts)
    external_url    VARCHAR(1000),              -- link to external resource or uploaded file
    language        VARCHAR(10) DEFAULT 'en',
    audience        VARCHAR(50) DEFAULT 'patient',  -- patient, caregiver, both
    tags            JSONB DEFAULT '[]',         -- searchable tags: ["diabetes", "insulin", "type2"]
    author          VARCHAR(255),
    source          VARCHAR(255),               -- ADA, AHA, CDC, Mayo Clinic, custom
    is_active       BOOLEAN NOT NULL DEFAULT true,
    view_count      INTEGER DEFAULT 0,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_education_material_org ON education_material(org_alias);
CREATE INDEX idx_education_material_category ON education_material(org_alias, category);
CREATE INDEX idx_education_material_type ON education_material(org_alias, content_type);
CREATE INDEX idx_education_material_tags ON education_material USING gin(tags);

-- ── 2. Patient Education Assignments ──
CREATE TABLE patient_education_assignment (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    patient_name    VARCHAR(255),
    material_id     BIGINT NOT NULL REFERENCES education_material(id) ON DELETE CASCADE,
    assigned_by     VARCHAR(255),
    assigned_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'assigned',  -- assigned, viewed, completed, dismissed
    viewed_at       TIMESTAMP,
    completed_at    TIMESTAMP,
    encounter_id    BIGINT,                    -- linked encounter (optional)
    notes           VARCHAR(500),              -- provider notes for patient
    patient_feedback TEXT,                     -- patient response/questions
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_education_assignment_org ON patient_education_assignment(org_alias);
CREATE INDEX idx_education_assignment_patient ON patient_education_assignment(org_alias, patient_id, status);
CREATE INDEX idx_education_assignment_material ON patient_education_assignment(material_id);
CREATE INDEX idx_education_assignment_status ON patient_education_assignment(org_alias, status);

-- ── 3. Menu Item ──
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'education', 'Education', 'BookOpen', '/education', 12);

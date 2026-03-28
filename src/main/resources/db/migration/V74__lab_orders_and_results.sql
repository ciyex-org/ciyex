-- V74: Lab orders and results tables
-- Replaces mock frontend data with real PostgreSQL-backed lab system

-- ============================================================
-- 1. Lab Orders
-- ============================================================
CREATE TABLE lab_order (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    order_number    VARCHAR(50) NOT NULL,
    order_name      VARCHAR(255),
    test_code       VARCHAR(100),
    test_display    VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'active',
    priority        VARCHAR(20) NOT NULL DEFAULT 'routine',
    order_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    order_date_time TIMESTAMP,
    lab_name        VARCHAR(255),
    ordering_provider VARCHAR(255),
    physician_name  VARCHAR(255),
    specimen_id     VARCHAR(100),
    diagnosis_code  TEXT,
    procedure_code  TEXT,
    result_status   VARCHAR(30) DEFAULT 'Pending',
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lab_order_org ON lab_order(org_alias);
CREATE INDEX idx_lab_order_patient ON lab_order(patient_id);
CREATE INDEX idx_lab_order_status ON lab_order(org_alias, status);
CREATE UNIQUE INDEX idx_lab_order_number ON lab_order(org_alias, order_number);

-- ============================================================
-- 2. Lab Results
-- ============================================================
CREATE TABLE lab_result (
    id              BIGSERIAL PRIMARY KEY,
    lab_order_id    BIGINT REFERENCES lab_order(id) ON DELETE SET NULL,
    patient_id      BIGINT NOT NULL,
    encounter_id    BIGINT,
    order_number    VARCHAR(50),
    procedure_name  VARCHAR(255),
    test_code       VARCHAR(100),
    test_name       VARCHAR(255),
    loinc_code      VARCHAR(20),
    status          VARCHAR(30) NOT NULL DEFAULT 'Pending',
    specimen        VARCHAR(100),
    collected_date  DATE,
    reported_date   DATE,
    abnormal_flag   VARCHAR(20),
    value           VARCHAR(255),
    numeric_value   NUMERIC(12,4),
    units           VARCHAR(50),
    reference_low   NUMERIC(12,4),
    reference_high  NUMERIC(12,4),
    reference_range VARCHAR(100),
    notes           TEXT,
    recommendations TEXT,
    signed          BOOLEAN DEFAULT FALSE,
    signed_at       TIMESTAMP,
    signed_by       VARCHAR(255),
    panel_name      VARCHAR(100),
    panel_code      VARCHAR(50),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lab_result_org ON lab_result(org_alias);
CREATE INDEX idx_lab_result_patient ON lab_result(patient_id);
CREATE INDEX idx_lab_result_order ON lab_result(lab_order_id);
CREATE INDEX idx_lab_result_loinc ON lab_result(loinc_code);
CREATE INDEX idx_lab_result_status ON lab_result(org_alias, status);
CREATE INDEX idx_lab_result_panel ON lab_result(panel_name);

-- ============================================================
-- 3. Lab Order Sets (predefined panels)
-- ============================================================
CREATE TABLE lab_order_set (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    description TEXT,
    tests       JSONB NOT NULL DEFAULT '[]',
    category    VARCHAR(50),
    active      BOOLEAN DEFAULT TRUE,
    org_alias   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lab_order_set_org ON lab_order_set(org_alias);

-- ============================================================
-- 4. Seed data: common lab panels
-- ============================================================
INSERT INTO lab_order_set (name, code, description, tests, category, org_alias) VALUES
('Complete Blood Count', 'CBC', 'Complete blood count with differential', '[
  {"testName":"WBC","testCode":"6690-2","units":"x10^3/uL","refLow":4.5,"refHigh":11.0},
  {"testName":"RBC","testCode":"789-8","units":"x10^6/uL","refLow":4.5,"refHigh":5.5},
  {"testName":"Hemoglobin","testCode":"718-7","units":"g/dL","refLow":12.0,"refHigh":17.5},
  {"testName":"Hematocrit","testCode":"4544-3","units":"%","refLow":36.0,"refHigh":51.0},
  {"testName":"Platelets","testCode":"777-3","units":"x10^3/uL","refLow":150,"refHigh":400},
  {"testName":"MCV","testCode":"787-2","units":"fL","refLow":80.0,"refHigh":100.0},
  {"testName":"MCH","testCode":"785-6","units":"pg","refLow":27.0,"refHigh":33.0},
  {"testName":"MCHC","testCode":"786-4","units":"g/dL","refLow":32.0,"refHigh":36.0},
  {"testName":"RDW","testCode":"788-0","units":"%","refLow":11.5,"refHigh":14.5},
  {"testName":"Neutrophils","testCode":"751-8","units":"%","refLow":40,"refHigh":70},
  {"testName":"Lymphocytes","testCode":"731-0","units":"%","refLow":20,"refHigh":40},
  {"testName":"Monocytes","testCode":"742-7","units":"%","refLow":2,"refHigh":8},
  {"testName":"Eosinophils","testCode":"711-2","units":"%","refLow":1,"refHigh":4},
  {"testName":"Basophils","testCode":"704-7","units":"%","refLow":0,"refHigh":1}
]', 'Hematology', '__GLOBAL__'),

('Basic Metabolic Panel', 'BMP', 'Basic metabolic panel (8 analytes)', '[
  {"testName":"Glucose","testCode":"2345-7","units":"mg/dL","refLow":70,"refHigh":100},
  {"testName":"BUN","testCode":"3094-0","units":"mg/dL","refLow":7,"refHigh":20},
  {"testName":"Creatinine","testCode":"2160-0","units":"mg/dL","refLow":0.7,"refHigh":1.3},
  {"testName":"Sodium","testCode":"2951-2","units":"mEq/L","refLow":136,"refHigh":145},
  {"testName":"Potassium","testCode":"2823-3","units":"mEq/L","refLow":3.5,"refHigh":5.0},
  {"testName":"Chloride","testCode":"2075-0","units":"mEq/L","refLow":98,"refHigh":106},
  {"testName":"CO2","testCode":"2028-9","units":"mEq/L","refLow":23,"refHigh":29},
  {"testName":"Calcium","testCode":"17861-6","units":"mg/dL","refLow":8.5,"refHigh":10.5}
]', 'Chemistry', '__GLOBAL__'),

('Comprehensive Metabolic Panel', 'CMP', 'Comprehensive metabolic panel (14 analytes)', '[
  {"testName":"Glucose","testCode":"2345-7","units":"mg/dL","refLow":70,"refHigh":100},
  {"testName":"BUN","testCode":"3094-0","units":"mg/dL","refLow":7,"refHigh":20},
  {"testName":"Creatinine","testCode":"2160-0","units":"mg/dL","refLow":0.7,"refHigh":1.3},
  {"testName":"Sodium","testCode":"2951-2","units":"mEq/L","refLow":136,"refHigh":145},
  {"testName":"Potassium","testCode":"2823-3","units":"mEq/L","refLow":3.5,"refHigh":5.0},
  {"testName":"Chloride","testCode":"2075-0","units":"mEq/L","refLow":98,"refHigh":106},
  {"testName":"CO2","testCode":"2028-9","units":"mEq/L","refLow":23,"refHigh":29},
  {"testName":"Calcium","testCode":"17861-6","units":"mg/dL","refLow":8.5,"refHigh":10.5},
  {"testName":"Total Protein","testCode":"2885-2","units":"g/dL","refLow":6.0,"refHigh":8.3},
  {"testName":"Albumin","testCode":"1751-7","units":"g/dL","refLow":3.5,"refHigh":5.5},
  {"testName":"Bilirubin Total","testCode":"1975-2","units":"mg/dL","refLow":0.1,"refHigh":1.2},
  {"testName":"ALP","testCode":"6768-6","units":"U/L","refLow":44,"refHigh":147},
  {"testName":"ALT","testCode":"1742-6","units":"U/L","refLow":7,"refHigh":56},
  {"testName":"AST","testCode":"1920-8","units":"U/L","refLow":10,"refHigh":40}
]', 'Chemistry', '__GLOBAL__'),

('Lipid Panel', 'LIPID', 'Lipid panel (fasting)', '[
  {"testName":"Total Cholesterol","testCode":"2093-3","units":"mg/dL","refLow":0,"refHigh":200},
  {"testName":"HDL Cholesterol","testCode":"2085-9","units":"mg/dL","refLow":40,"refHigh":60},
  {"testName":"LDL Cholesterol","testCode":"2089-1","units":"mg/dL","refLow":0,"refHigh":100},
  {"testName":"Triglycerides","testCode":"2571-8","units":"mg/dL","refLow":0,"refHigh":150},
  {"testName":"VLDL","testCode":"2090-9","units":"mg/dL","refLow":5,"refHigh":40}
]', 'Chemistry', '__GLOBAL__'),

('Thyroid Panel', 'THYROID', 'Thyroid function panel', '[
  {"testName":"TSH","testCode":"3016-3","units":"mIU/L","refLow":0.4,"refHigh":4.0},
  {"testName":"Free T4","testCode":"3024-7","units":"ng/dL","refLow":0.8,"refHigh":1.8},
  {"testName":"Free T3","testCode":"3051-0","units":"pg/mL","refLow":2.3,"refHigh":4.2}
]', 'Endocrine', '__GLOBAL__'),

('Hemoglobin A1c', 'HBA1C', 'Glycated hemoglobin', '[
  {"testName":"HbA1c","testCode":"4548-4","units":"%","refLow":4.0,"refHigh":5.6}
]', 'Chemistry', '__GLOBAL__'),

('Urinalysis', 'UA', 'Complete urinalysis', '[
  {"testName":"Color","testCode":"5778-6","units":"","refLow":null,"refHigh":null},
  {"testName":"Clarity","testCode":"32167-9","units":"","refLow":null,"refHigh":null},
  {"testName":"Specific Gravity","testCode":"2965-2","units":"","refLow":1.005,"refHigh":1.030},
  {"testName":"pH","testCode":"2756-5","units":"","refLow":4.5,"refHigh":8.0},
  {"testName":"Protein","testCode":"2888-6","units":"mg/dL","refLow":null,"refHigh":null},
  {"testName":"Glucose (Urine)","testCode":"2350-7","units":"mg/dL","refLow":null,"refHigh":null},
  {"testName":"Ketones","testCode":"2514-8","units":"mg/dL","refLow":null,"refHigh":null},
  {"testName":"Blood","testCode":"5794-3","units":"","refLow":null,"refHigh":null},
  {"testName":"WBC (Urine)","testCode":"5821-4","units":"/hpf","refLow":0,"refHigh":5},
  {"testName":"Bacteria","testCode":"25145-4","units":"","refLow":null,"refHigh":null}
]', 'Urinalysis', '__GLOBAL__'),

('Coagulation Panel', 'COAG', 'PT/INR and aPTT', '[
  {"testName":"PT","testCode":"5902-2","units":"sec","refLow":11.0,"refHigh":13.5},
  {"testName":"INR","testCode":"6301-6","units":"","refLow":0.8,"refHigh":1.2},
  {"testName":"aPTT","testCode":"3173-2","units":"sec","refLow":25,"refHigh":35}
]', 'Hematology', '__GLOBAL__'),

('Liver Function Tests', 'LFT', 'Hepatic function panel', '[
  {"testName":"ALT","testCode":"1742-6","units":"U/L","refLow":7,"refHigh":56},
  {"testName":"AST","testCode":"1920-8","units":"U/L","refLow":10,"refHigh":40},
  {"testName":"ALP","testCode":"6768-6","units":"U/L","refLow":44,"refHigh":147},
  {"testName":"Total Bilirubin","testCode":"1975-2","units":"mg/dL","refLow":0.1,"refHigh":1.2},
  {"testName":"Direct Bilirubin","testCode":"1968-7","units":"mg/dL","refLow":0.0,"refHigh":0.3},
  {"testName":"Albumin","testCode":"1751-7","units":"g/dL","refLow":3.5,"refHigh":5.5},
  {"testName":"Total Protein","testCode":"2885-2","units":"g/dL","refLow":6.0,"refHigh":8.3},
  {"testName":"GGT","testCode":"2324-2","units":"U/L","refLow":9,"refHigh":48}
]', 'Chemistry', '__GLOBAL__'),

('Renal Panel', 'RENAL', 'Kidney function tests', '[
  {"testName":"BUN","testCode":"3094-0","units":"mg/dL","refLow":7,"refHigh":20},
  {"testName":"Creatinine","testCode":"2160-0","units":"mg/dL","refLow":0.7,"refHigh":1.3},
  {"testName":"eGFR","testCode":"33914-3","units":"mL/min/1.73m2","refLow":60,"refHigh":null},
  {"testName":"Sodium","testCode":"2951-2","units":"mEq/L","refLow":136,"refHigh":145},
  {"testName":"Potassium","testCode":"2823-3","units":"mEq/L","refLow":3.5,"refHigh":5.0},
  {"testName":"Phosphorus","testCode":"2777-1","units":"mg/dL","refLow":2.5,"refHigh":4.5},
  {"testName":"Uric Acid","testCode":"3084-1","units":"mg/dL","refLow":3.0,"refHigh":7.0}
]', 'Chemistry', '__GLOBAL__');

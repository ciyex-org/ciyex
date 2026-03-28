-- Practice types, specialties, tab configuration, and custom tabs

-- =====================================================
-- Tables
-- =====================================================

CREATE TABLE practice_type (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    category        VARCHAR(100)  NOT NULL DEFAULT 'MEDICAL',
    description     TEXT,
    icon            VARCHAR(100)  DEFAULT 'Stethoscope',
    active          BOOLEAN       NOT NULL DEFAULT true,
    org_id          VARCHAR(100)  NOT NULL DEFAULT '*',
    default_tab_config JSONB,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (code, org_id)
);

CREATE TABLE specialty (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    description     TEXT,
    icon            VARCHAR(100)  DEFAULT 'Stethoscope',
    parent_code     VARCHAR(100),
    active          BOOLEAN       NOT NULL DEFAULT true,
    org_id          VARCHAR(100)  NOT NULL DEFAULT '*',
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (code, org_id)
);

CREATE TABLE practice_type_specialty (
    practice_type_code VARCHAR(100) NOT NULL,
    specialty_code     VARCHAR(100) NOT NULL,
    PRIMARY KEY (practice_type_code, specialty_code)
);

CREATE TABLE tab_config (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              VARCHAR(100)  NOT NULL,
    practice_type_code  VARCHAR(100),
    tab_config          JSONB         NOT NULL DEFAULT '[]',
    source              VARCHAR(50)   NOT NULL DEFAULT 'UNIVERSAL_DEFAULT',
    created_at          TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (org_id)
);

CREATE TABLE custom_tab (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          VARCHAR(100)  NOT NULL,
    tab_key         VARCHAR(100)  NOT NULL,
    label           VARCHAR(255)  NOT NULL,
    icon            VARCHAR(100)  DEFAULT 'FileText',
    category        VARCHAR(100)  DEFAULT 'Other',
    form_schema     JSONB         NOT NULL DEFAULT '{}',
    position        INT           NOT NULL DEFAULT 0,
    active          BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (org_id, tab_key)
);

CREATE INDEX idx_practice_type_org    ON practice_type(org_id);
CREATE INDEX idx_specialty_org        ON specialty(org_id);
CREATE INDEX idx_tab_config_org       ON tab_config(org_id);
CREATE INDEX idx_custom_tab_org       ON custom_tab(org_id);

-- =====================================================
-- Row Level Security
-- =====================================================
ALTER TABLE practice_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE specialty ENABLE ROW LEVEL SECURITY;
ALTER TABLE tab_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_tab ENABLE ROW LEVEL SECURITY;

CREATE POLICY practice_type_tenant_policy ON practice_type
    USING (org_id = '*' OR org_id = current_setting('app.current_org', true));
CREATE POLICY specialty_tenant_policy ON specialty
    USING (org_id = '*' OR org_id = current_setting('app.current_org', true));
CREATE POLICY tab_config_tenant_policy ON tab_config
    USING (org_id = current_setting('app.current_org', true) OR org_id = '*');
CREATE POLICY custom_tab_tenant_policy ON custom_tab
    USING (org_id = current_setting('app.current_org', true));

ALTER TABLE practice_type FORCE ROW LEVEL SECURITY;
ALTER TABLE specialty FORCE ROW LEVEL SECURITY;
ALTER TABLE tab_config FORCE ROW LEVEL SECURITY;
ALTER TABLE custom_tab FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON practice_type, specialty, tab_config, custom_tab, practice_type_specialty TO app_user;

-- =====================================================
-- Seed: Practice Types
-- =====================================================
INSERT INTO practice_type (code, name, category, description, icon, default_tab_config) VALUES
('general-practice', 'General Practice', 'MEDICAL',
 'Primary care and family medicine',
 'Stethoscope',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":4}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"lab-results","label":"Lab Results","icon":"TestTube","visible":true,"position":2},{"key":"immunizations","label":"Immunizations","icon":"Syringe","visible":true,"position":3},{"key":"procedures","label":"Procedures","icon":"Scissors","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]'),
('internal-medicine', 'Internal Medicine', 'MEDICAL',
 'Adult internal medicine and subspecialties',
 'Heart',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":4}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"lab-results","label":"Lab Results","icon":"TestTube","visible":true,"position":2},{"key":"immunizations","label":"Immunizations","icon":"Syringe","visible":true,"position":3},{"key":"procedures","label":"Procedures","icon":"Scissors","visible":true,"position":4},{"key":"imaging","label":"Imaging","icon":"Scan","visible":true,"position":5}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]'),
('pediatrics', 'Pediatrics', 'MEDICAL',
 'Pediatric and adolescent medicine',
 'Baby',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"growth-chart","label":"Growth Chart","icon":"TrendingUp","visible":true,"position":3},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":4},{"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":5}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"immunizations","label":"Immunizations","icon":"Syringe","visible":true,"position":2},{"key":"lab-results","label":"Lab Results","icon":"TestTube","visible":true,"position":3},{"key":"developmental","label":"Developmental","icon":"Brain","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]'),
('orthopedic-surgery', 'Orthopedic Surgery', 'SURGICAL',
 'Orthopedic and musculoskeletal surgery',
 'Bone',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"surgical-history","label":"Surgical History","icon":"Scissors","visible":true,"position":4}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"imaging","label":"Imaging","icon":"Scan","visible":true,"position":2},{"key":"procedures","label":"Procedures","icon":"Scissors","visible":true,"position":3},{"key":"physical-therapy","label":"Physical Therapy","icon":"Dumbbell","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2},{"key":"prior-auth","label":"Prior Auth","icon":"ShieldCheck","visible":true,"position":3}]}]'),
('general-surgery', 'General Surgery', 'SURGICAL',
 'General surgical practice',
 'Scissors',
 NULL),
('psychiatry', 'Psychiatry', 'BEHAVIORAL',
 'Psychiatric and mental health services',
 'Brain',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":4}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"assessments","label":"Assessments","icon":"ClipboardCheck","visible":true,"position":2},{"key":"treatment-plans","label":"Treatment Plans","icon":"Target","visible":true,"position":3},{"key":"progress-notes","label":"Progress Notes","icon":"FileEdit","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]'),
('psychology', 'Psychology', 'BEHAVIORAL',
 'Clinical psychology and counseling',
 'Brain',
 NULL),
('general-dentistry', 'General Dentistry', 'DENTAL',
 'General dental practice',
 'SmilePlus',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":2},{"key":"dental-history","label":"Dental History","icon":"FileText","visible":true,"position":3}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"dental-chart","label":"Dental Chart","icon":"Grid","visible":true,"position":1},{"key":"treatment-plan","label":"Treatment Plan","icon":"Target","visible":true,"position":2},{"key":"imaging","label":"X-Rays","icon":"Scan","visible":true,"position":3},{"key":"perio-chart","label":"Perio Chart","icon":"BarChart","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]'),
('physical-therapy', 'Physical Therapy', 'ALLIED_HEALTH',
 'Physical therapy and rehabilitation',
 'Dumbbell',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"evaluations","label":"Evaluations","icon":"ClipboardCheck","visible":true,"position":1},{"key":"treatment-plans","label":"Treatment Plans","icon":"Target","visible":true,"position":2},{"key":"exercise-log","label":"Exercise Log","icon":"Dumbbell","visible":true,"position":3},{"key":"progress-notes","label":"Progress Notes","icon":"FileEdit","visible":true,"position":4},{"key":"goals","label":"Goals","icon":"Flag","visible":true,"position":5}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2},{"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2},{"key":"authorization","label":"Authorization","icon":"ShieldCheck","visible":true,"position":3}]}]'),
('occupational-therapy', 'Occupational Therapy', 'ALLIED_HEALTH',
 'Occupational therapy services',
 'Hand',
 NULL),
('home-health', 'Home Health', 'HOME_HEALTH',
 'Home health nursing and aide services',
 'Home',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"care-plan","label":"Care Plan","icon":"ClipboardList","visible":true,"position":4}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Visits","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"oasis","label":"OASIS","icon":"FileCheck","visible":true,"position":2},{"key":"wound-care","label":"Wound Care","icon":"Bandage","visible":true,"position":3},{"key":"progress-notes","label":"Progress Notes","icon":"FileEdit","visible":true,"position":4}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2},{"key":"authorization","label":"Authorization","icon":"ShieldCheck","visible":true,"position":3}]}]'),
('hospitalist', 'Hospitalist', 'INPATIENT',
 'Inpatient hospital medicine',
 'Building2',
 '[{"label":"Overview","position":0,"tabs":[{"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},{"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},{"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},{"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},{"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":4},{"key":"admission-info","label":"Admission","icon":"BedDouble","visible":true,"position":5}]},{"label":"Clinical","position":1,"tabs":[{"key":"encounters","label":"Encounters","icon":"FileText","visible":true,"position":0},{"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":1},{"key":"orders","label":"Orders","icon":"ClipboardList","visible":true,"position":2},{"key":"lab-results","label":"Lab Results","icon":"TestTube","visible":true,"position":3},{"key":"imaging","label":"Imaging","icon":"Scan","visible":true,"position":4},{"key":"procedures","label":"Procedures","icon":"Scissors","visible":true,"position":5}]},{"label":"General","position":2,"tabs":[{"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},{"key":"consults","label":"Consults","icon":"Users","visible":true,"position":1},{"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2}]},{"label":"Financial","position":3,"tabs":[{"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},{"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":1},{"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":2}]}]');

-- =====================================================
-- Seed: Specialties
-- =====================================================
INSERT INTO specialty (code, name, description, icon, parent_code) VALUES
('family-medicine',          'Family Medicine',           'Primary care for all ages',                  'Stethoscope',     NULL),
('internal-medicine',        'Internal Medicine',         'Adult medicine',                             'Heart',           NULL),
('pediatrics',               'Pediatrics',                'Child and adolescent medicine',              'Baby',            NULL),
('cardiology',               'Cardiology',                'Heart and cardiovascular system',            'Heart',           'internal-medicine'),
('dermatology',              'Dermatology',               'Skin conditions and diseases',               'Scan',            NULL),
('endocrinology',            'Endocrinology',             'Hormone and metabolic disorders',            'Pill',            'internal-medicine'),
('gastroenterology',         'Gastroenterology',          'Digestive system disorders',                 'Stethoscope',     'internal-medicine'),
('neurology',                'Neurology',                 'Brain and nervous system',                   'Brain',           NULL),
('obstetrics-gynecology',    'Obstetrics & Gynecology',   'Women''s reproductive health',               'Heart',           NULL),
('oncology',                 'Oncology',                  'Cancer diagnosis and treatment',             'Shield',          'internal-medicine'),
('ophthalmology',            'Ophthalmology',             'Eye diseases and surgery',                   'Eye',             NULL),
('orthopedics',              'Orthopedics',               'Musculoskeletal system',                     'Bone',            NULL),
('otolaryngology',           'Otolaryngology (ENT)',      'Ear, nose, and throat',                      'Ear',             NULL),
('pulmonology',              'Pulmonology',               'Respiratory system',                         'Wind',            'internal-medicine'),
('radiology',                'Radiology',                 'Diagnostic imaging',                         'Scan',            NULL),
('rheumatology',             'Rheumatology',              'Autoimmune and joint diseases',              'Bone',            'internal-medicine'),
('urology',                  'Urology',                   'Urinary tract and male reproductive system', 'Stethoscope',     NULL),
('psychiatry',               'Psychiatry',                'Mental health and psychiatric disorders',    'Brain',           NULL),
('psychology',               'Psychology',                'Clinical psychology and counseling',          'Brain',           NULL),
('general-surgery',          'General Surgery',           'General surgical procedures',                'Scissors',        NULL),
('oral-surgery',             'Oral & Maxillofacial Surgery', 'Jaw and facial surgery',                  'Scissors',        NULL),
('general-dentistry',        'General Dentistry',         'General dental care',                        'SmilePlus',       NULL),
('orthodontics',             'Orthodontics',              'Teeth alignment and braces',                 'SmilePlus',       NULL),
('periodontics',             'Periodontics',              'Gum disease treatment',                      'SmilePlus',       NULL),
('physical-therapy',         'Physical Therapy',          'Movement and rehabilitation',                'Dumbbell',        NULL),
('occupational-therapy',     'Occupational Therapy',      'Daily living skills rehabilitation',          'Hand',            NULL),
('speech-therapy',           'Speech-Language Pathology',  'Speech and swallowing disorders',            'MessageCircle',   NULL),
('home-health-nursing',      'Home Health Nursing',       'In-home nursing care',                       'Home',            NULL),
('hospitalist-medicine',     'Hospitalist Medicine',      'Inpatient hospital care',                    'Building2',       NULL),
('emergency-medicine',       'Emergency Medicine',        'Emergency and acute care',                   'Siren',           NULL);

-- =====================================================
-- Seed: Practice Type ↔ Specialty mapping
-- =====================================================
INSERT INTO practice_type_specialty (practice_type_code, specialty_code) VALUES
('general-practice',     'family-medicine'),
('general-practice',     'internal-medicine'),
('internal-medicine',    'internal-medicine'),
('internal-medicine',    'cardiology'),
('internal-medicine',    'endocrinology'),
('internal-medicine',    'gastroenterology'),
('internal-medicine',    'pulmonology'),
('internal-medicine',    'rheumatology'),
('internal-medicine',    'oncology'),
('pediatrics',           'pediatrics'),
('orthopedic-surgery',   'orthopedics'),
('general-surgery',      'general-surgery'),
('psychiatry',           'psychiatry'),
('psychiatry',           'psychology'),
('psychology',           'psychology'),
('general-dentistry',    'general-dentistry'),
('general-dentistry',    'orthodontics'),
('general-dentistry',    'periodontics'),
('general-dentistry',    'oral-surgery'),
('physical-therapy',     'physical-therapy'),
('occupational-therapy', 'occupational-therapy'),
('home-health',          'home-health-nursing'),
('hospitalist',          'hospitalist-medicine'),
('hospitalist',          'emergency-medicine');

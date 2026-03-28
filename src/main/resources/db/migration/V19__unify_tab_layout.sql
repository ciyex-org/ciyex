-- V19: Unify tab_config layout into tab_field_config
-- Adds layout columns (label, icon, category, position, visible) to tab_field_config,
-- making it the single source of truth for both tab layout AND field definitions.
-- Deprecates the separate tab_config table.

-- 1. Add layout columns
ALTER TABLE tab_field_config
  ADD COLUMN IF NOT EXISTS label    VARCHAR(255),
  ADD COLUMN IF NOT EXISTS icon     VARCHAR(100) DEFAULT 'FileText',
  ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'Other',
  ADD COLUMN IF NOT EXISTS position INT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS visible  BOOLEAN DEFAULT true;

-- 2. Create dashboard entry (no FHIR resources, just layout metadata)
INSERT INTO tab_field_config (id, tab_key, practice_type_code, org_id, fhir_resources, field_config,
  label, icon, category, position, visible)
VALUES (gen_random_uuid(), 'dashboard', '*', '*', '[]', '{}',
  'Dashboard', 'LayoutDashboard', 'Overview', 0, true)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
  SET label = 'Dashboard', icon = 'LayoutDashboard', category = 'Overview', position = 0, visible = true;

-- 3. Populate layout metadata for all existing universal tab_field_config rows

-- === Overview ===
UPDATE tab_field_config SET label='Demographics', icon='UserRound', category='Overview', position=1
WHERE tab_key='demographics' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Insurance', icon='ShieldCheck', category='Overview', position=2
WHERE tab_key='insurance' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Vitals', icon='Activity', category='Overview', position=3
WHERE tab_key='vitals' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Allergies', icon='ShieldAlert', category='Overview', position=4
WHERE tab_key='allergies' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Problems', icon='HeartPulse', category='Overview', position=5
WHERE tab_key='medicalproblems' AND org_id='*' AND practice_type_code='*';

-- === Encounters ===
UPDATE tab_field_config SET label='Encounters', icon='Stethoscope', category='Encounters', position=0
WHERE tab_key='encounters' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Appointments', icon='CalendarDays', category='Encounters', position=1
WHERE tab_key='appointments' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Visit Notes', icon='FileText', category='Encounters', position=2
WHERE tab_key='visit-notes' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Referrals', icon='Forward', category='Encounters', position=3
WHERE tab_key='referrals' AND org_id='*' AND practice_type_code='*';

-- === Clinical ===
UPDATE tab_field_config SET label='Medications', icon='Pill', category='Clinical', position=0
WHERE tab_key='medications' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Labs', icon='FlaskConical', category='Clinical', position=1
WHERE tab_key='labs' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Immunizations', icon='Syringe', category='Clinical', position=2
WHERE tab_key='immunizations' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Procedures', icon='Scissors', category='Clinical', position=3
WHERE tab_key='procedures' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='History', icon='Clock', category='Clinical', position=4
WHERE tab_key='history' AND org_id='*' AND practice_type_code='*';

-- === Claims ===
UPDATE tab_field_config SET label='Billing', icon='Receipt', category='Claims', position=0
WHERE tab_key='billing' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Claims', icon='ClipboardList', category='Claims', position=1
WHERE tab_key='claims' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Submissions', icon='Forward', category='Claims', position=2
WHERE tab_key='claim-submissions' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Denials', icon='CircleAlert', category='Claims', position=3
WHERE tab_key='claim-denials' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='ERA / Remittance', icon='FileCheck', category='Claims', position=4
WHERE tab_key='era-remittance' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Transactions', icon='ArrowLeftRight', category='Claims', position=5
WHERE tab_key='transactions' AND org_id='*' AND practice_type_code='*';

-- === General ===
UPDATE tab_field_config SET label='Documents', icon='FileText', category='General', position=0
WHERE tab_key='documents' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Education', icon='GraduationCap', category='General', position=1
WHERE tab_key='education' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Messaging', icon='MessageSquare', category='General', position=2
WHERE tab_key='messaging' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Relationships', icon='Users', category='General', position=3
WHERE tab_key='relationships' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Services', icon='Building2', category='General', position=4
WHERE tab_key='healthcareservices' AND org_id='*' AND practice_type_code='*';

-- === Financial ===
UPDATE tab_field_config SET label='Payment', icon='CreditCard', category='Financial', position=0
WHERE tab_key='payment' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Payments', icon='CreditCard', category='Financial', position=1
WHERE tab_key='payments' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Statements', icon='FileBarChart', category='Financial', position=2
WHERE tab_key='statements' AND org_id='*' AND practice_type_code='*';

-- === Other ===
UPDATE tab_field_config SET label='Issues', icon='CircleAlert', category='Other', position=0
WHERE tab_key='issues' AND org_id='*' AND practice_type_code='*';

UPDATE tab_field_config SET label='Report', icon='FileBarChart', category='Other', position=1
WHERE tab_key='report' AND org_id='*' AND practice_type_code='*';

-- 4. Index for layout queries (category + position ordering)
CREATE INDEX IF NOT EXISTS idx_tfc_layout
ON tab_field_config (org_id, practice_type_code, category, position);

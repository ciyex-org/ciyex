-- Add api_base_path column to tab_field_config
-- Settings pages use this to know which API endpoint to call for CRUD operations

ALTER TABLE tab_field_config ADD COLUMN IF NOT EXISTS api_base_path VARCHAR(255);

-- Seed tab_field_config rows for settings pages (universal defaults)
-- These provide the apiBasePath + FHIR resource mappings for GenericSettingsPage

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible, api_base_path)
VALUES
  ('providers',           '*', '*', '[{"type":"Practitioner"}]',      '{}', 'Providers',            'UserCog',     'Settings', 0, 0,  true, '/api/providers'),
  ('referral-providers',  '*', '*', '[{"type":"Practitioner"}]',      '{}', 'Referral Providers',   'UserPlus',    'Settings', 0, 1,  true, '/api/referral-providers'),
  ('referral-practices',  '*', '*', '[{"type":"Organization"}]',      '{}', 'Referral Practices',   'Building',    'Settings', 0, 2,  true, '/api/referral-practices'),
  ('insurance',           '*', '*', '[{"type":"Organization"},{"type":"Coverage"}]', '{}', 'Insurance Companies', 'Shield', 'Settings', 0, 3, true, '/api/insurance-companies'),
  ('template-documents',  '*', '*', '[{"type":"DocumentReference"}]', '{}', 'Template Documents',   'FilePlus',    'Settings', 0, 5,  true, '/api/template-documents'),
  ('codes',               '*', '*', '[{"type":"CodeSystem"},{"type":"ValueSet"}]', '{}', 'Codes',    'FileCode',    'Settings', 0, 6,  true, '/api/codes'),
  ('integration',         '*', '*', '[{"type":"Endpoint"}]',          '{}', 'Integration',          'Plug',        'Settings', 0, 7,  true, '/api/integration'),
  ('services',            '*', '*', '[{"type":"HealthcareService"}]', '{}', 'Services',             'Briefcase',   'Settings', 0, 8,  true, '/api/services'),
  ('forms',               '*', '*', '[{"type":"Questionnaire"}]',     '{}', 'Forms',                'FileInput',   'Settings', 0, 10, true, '/api/forms'),
  ('facilities',          '*', '*', '[{"type":"Location"}]',          '{}', 'Facilities',           'Building2',   'Settings', 0, 11, true, '/api/facilities'),
  ('practice',            '*', '*', '[{"type":"Organization"}]',      '{}', 'Practice',             'Stethoscope', 'Settings', 0, 12, true, '/api/practice'),
  ('fee-schedules',       '*', '*', '[]',                             '{}', 'Fee Schedules',        'DollarSign',  'Settings', 0, 13, true, '/api/fee-schedules')
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET api_base_path = EXCLUDED.api_base_path;

-- Update existing patient chart tab_field_config rows that share the same tab_key
-- (documents, billing, insurance already exist from V5 for patient chart tabs)
-- Only set api_base_path where it's currently NULL (don't override if already set)
UPDATE tab_field_config SET api_base_path = '/api/documents'        WHERE tab_key = 'documents'  AND api_base_path IS NULL;
UPDATE tab_field_config SET api_base_path = '/api/invoice-bills'    WHERE tab_key = 'billing'    AND api_base_path IS NULL;

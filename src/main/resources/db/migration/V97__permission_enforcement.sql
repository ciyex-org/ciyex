-- =========================================================================
-- V97: Permission enforcement — add required_permission to tab_field_config
--      and menu_item for RBAC enforcement
-- =========================================================================

-- A) Add required_permission to tab_field_config
ALTER TABLE tab_field_config ADD COLUMN IF NOT EXISTS required_permission VARCHAR(50);

-- Map tab_keys → permission categories
-- Patient chart: demographics
UPDATE tab_field_config SET required_permission = 'demographics'
  WHERE tab_key IN ('demographics', 'relationships', 'insurance', 'insurance-coverage', 'portal-demographics')
    AND required_permission IS NULL;

-- Scheduling
UPDATE tab_field_config SET required_permission = 'scheduling'
  WHERE tab_key IN ('appointments')
    AND required_permission IS NULL;

-- Clinical chart
UPDATE tab_field_config SET required_permission = 'chart'
  WHERE tab_key IN ('vitals', 'allergies', 'medicalproblems', 'encounters', 'procedures',
    'referrals', 'history', 'visit-notes', 'education', 'encounter-form')
    AND required_permission IS NULL;

-- Orders / Labs
UPDATE tab_field_config SET required_permission = 'orders'
  WHERE tab_key IN ('labs', 'lab-results')
    AND required_permission IS NULL;

-- Prescriptions / Medications
UPDATE tab_field_config SET required_permission = 'rx'
  WHERE tab_key IN ('medications', 'immunizations')
    AND required_permission IS NULL;

-- Documents
UPDATE tab_field_config SET required_permission = 'documents'
  WHERE tab_key IN ('documents', 'template-documents')
    AND required_permission IS NULL;

-- Billing / Financial
UPDATE tab_field_config SET required_permission = 'billing'
  WHERE tab_key IN ('billing', 'claims', 'claim-submissions', 'claim-denials',
    'era-remittance', 'transactions', 'payment', 'payments', 'statements')
    AND required_permission IS NULL;

-- Messaging
UPDATE tab_field_config SET required_permission = 'messaging'
  WHERE tab_key IN ('messaging')
    AND required_permission IS NULL;

-- Reports
UPDATE tab_field_config SET required_permission = 'reports'
  WHERE tab_key IN ('report', 'issues')
    AND required_permission IS NULL;

-- Settings / Admin
UPDATE tab_field_config SET required_permission = 'admin'
  WHERE tab_key IN ('providers', 'facilities', 'practice', 'services',
    'healthcareservices', 'referral-practices', 'referral-providers')
    AND required_permission IS NULL;


-- B) Add required_permission to menu_item (for sidebar filtering)
ALTER TABLE menu_item ADD COLUMN IF NOT EXISTS required_permission VARCHAR(50);

-- Scheduling items
UPDATE menu_item SET required_permission = 'scheduling'
  WHERE item_key IN ('calendar', 'appointments')
    AND required_permission IS NULL;

-- Clinical items
UPDATE menu_item SET required_permission = 'chart'
  WHERE item_key IN ('encounters', 'clinical', 'prescriptions', 'labs', 'immunizations',
    'referrals', 'authorizations', 'care-plans', 'education', 'clinical-alerts')
    AND required_permission IS NULL;

-- Demographics
UPDATE menu_item SET required_permission = 'demographics'
  WHERE item_key IN ('patients')
    AND required_permission IS NULL;

-- Messaging / Notifications
UPDATE menu_item SET required_permission = 'messaging'
  WHERE item_key IN ('messaging', 'notifications', 'fax')
    AND required_permission IS NULL;

-- Billing / Payments
UPDATE menu_item SET required_permission = 'billing'
  WHERE item_key IN ('payments', 'claim-management', 'operations')
    AND required_permission IS NULL;

-- Administration / Settings
UPDATE menu_item SET required_permission = 'admin'
  WHERE item_key IN ('administration', 'users', 'roles-permissions', 'settings',
    'settings-general', 'settings-layout', 'settings-portal')
    AND required_permission IS NULL;

-- Reports
UPDATE menu_item SET required_permission = 'reports'
  WHERE item_key IN ('reports')
    AND required_permission IS NULL;

-- Documents
UPDATE menu_item SET required_permission = 'documents'
  WHERE item_key IN ('doc-scanning')
    AND required_permission IS NULL;

-- The following items have NULL required_permission (visible to all authenticated users):
-- tasks, recall, codes, inventory, consents, kiosk, audit-log, hub, dev-portal, system

-- V100: Fix PATIENT role EHR permissions and plug gaps in required_permission mapping
--
-- Issues:
--   1. Several tab_keys lack required_permission → any authenticated user can access them
--   2. PATIENT role has demographics.read + chart.read → can browse ALL patients in EHR
--      PATIENT users should use the Portal (PortalGenericResourceService), not EHR.
--   3. menu_item 'patients' lacks required_permission → shows in PATIENT's sidebar

-- ─── 1. Fill gaps in tab_field_config required_permission ──────────────────────────

-- Patient list/search tabs require demographics permission
UPDATE tab_field_config SET required_permission = 'demographics'
  WHERE tab_key IN ('patients', 'patient-search', 'patient-list')
  AND required_permission IS NULL;

-- Encounter-related sub-tabs require chart permission
UPDATE tab_field_config SET required_permission = 'chart'
  WHERE tab_key IN ('soap-notes', 'intake', 'clinical-notes', 'care-plan',
    'clinical-decision', 'problem-list', 'past-medical-history')
  AND required_permission IS NULL;

-- Scheduling-related tabs
UPDATE tab_field_config SET required_permission = 'scheduling'
  WHERE tab_key IN ('appointment-detail', 'schedule')
  AND required_permission IS NULL;

-- ─── 2. Fix menu_item required_permission gaps ─────────────────────────────────────

UPDATE menu_item SET required_permission = 'demographics'
  WHERE item_key IN ('patients', 'patient-search')
  AND required_permission IS NULL;

-- ─── 3. Fix PATIENT role: remove EHR-browsing permissions from __SYSTEM__ template ─
--
-- PATIENT users should use the Portal for their own data access.
-- In the EHR, PATIENT role should have no demographics/chart browsing rights.
-- Portal routes (/api/portal/*) are independent and unaffected by this.

UPDATE role_permission_config
SET permissions = '["messaging.read", "messaging.send", "documents.read"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'PATIENT';

-- Also update any already-provisioned org-specific PATIENT roles to match
UPDATE role_permission_config
SET permissions = '["messaging.read", "messaging.send", "documents.read"]'::jsonb
WHERE role_name = 'PATIENT'
  AND org_alias != '__SYSTEM__'
  AND is_system = true;

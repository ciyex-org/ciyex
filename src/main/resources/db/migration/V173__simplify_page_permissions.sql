-- V100: Simplify page permissions — remove redundant FHIR page-level checks
--
-- FHIR resources (scheduling, demographics, chart) are already access-controlled
-- by the FHIR server via SMART scopes. Duplicating this at the page level adds
-- complexity with no security benefit. Only non-FHIR pages need page permissions.
--
-- Settings is opened to all roles — individual settings tabs are gated by FHIR
-- write scopes (e.g. staff can read org/provider but only write insurance/referral).
-- Admin-only tabs (Users, Roles) are gated in the frontend.

-- Remove page permission for FHIR-backed pages (FHIR server enforces access)
UPDATE menu_item SET required_permission = NULL WHERE required_permission = 'scheduling';
UPDATE menu_item SET required_permission = NULL WHERE required_permission = 'demographics';
UPDATE menu_item SET required_permission = NULL WHERE required_permission = 'chart';

-- Open Settings to all roles (was admin-only)
-- Staff need read-only access to org/provider and write access to insurance/referral
UPDATE menu_item SET required_permission = NULL WHERE required_permission = 'admin';

-- V174: Complete FHIR permission cleanup
--
-- V173 removed page permissions for scheduling, demographics, chart, admin.
-- This migration completes the cleanup for ALL FHIR-backed pages:
-- orders, rx, billing, documents.
--
-- FHIR server enforces SMART scopes — page-level gating is redundant.
-- Only non-FHIR pages keep required_permission: messaging, reports.
--
-- Also: remove required_permission from FHIR-backed tab_field_configs
-- and add Organization.write + Practitioner.write to staff roles so they
-- can add insurance orgs, referral orgs, and referral providers in Settings.

-- ─── A. Menu items: remove page permission for remaining FHIR pages ───
UPDATE menu_item SET required_permission = NULL
WHERE required_permission IN ('orders', 'rx', 'billing', 'documents');

-- ─── B. tab_field_config: remove required_permission for FHIR-backed tabs ───
-- FHIR server enforces access; tab_field_config permission was never checked properly.
-- Keep required_permission only for non-FHIR tabs (messaging, reports).
UPDATE tab_field_config SET required_permission = NULL
WHERE required_permission IN ('scheduling', 'demographics', 'chart', 'orders', 'rx',
                              'billing', 'documents', 'admin');

-- ─── C. Add Organization.write + Practitioner.write to staff roles ───
-- Staff need to add insurance orgs, referral orgs, referral providers in Settings.

-- FRONT_DESK
UPDATE role_permission_config
SET smart_scopes = (
    SELECT jsonb_agg(DISTINCT val)
    FROM jsonb_array_elements(
        smart_scopes || '["SCOPE_user/Organization.write", "SCOPE_user/Practitioner.write", "SCOPE_user/Coverage.write"]'::jsonb
    ) AS val
)
WHERE org_alias = '__SYSTEM__' AND role_name = 'FRONT_DESK';

-- MA
UPDATE role_permission_config
SET smart_scopes = (
    SELECT jsonb_agg(DISTINCT val)
    FROM jsonb_array_elements(
        smart_scopes || '["SCOPE_user/Organization.write", "SCOPE_user/Practitioner.write"]'::jsonb
    ) AS val
)
WHERE org_alias = '__SYSTEM__' AND role_name = 'MA';

-- NURSE
UPDATE role_permission_config
SET smart_scopes = (
    SELECT jsonb_agg(DISTINCT val)
    FROM jsonb_array_elements(
        smart_scopes || '["SCOPE_user/Organization.write", "SCOPE_user/Practitioner.write"]'::jsonb
    ) AS val
)
WHERE org_alias = '__SYSTEM__' AND role_name = 'NURSE';

-- BILLING
UPDATE role_permission_config
SET smart_scopes = (
    SELECT jsonb_agg(DISTINCT val)
    FROM jsonb_array_elements(
        smart_scopes || '["SCOPE_user/Organization.write", "SCOPE_user/Practitioner.write"]'::jsonb
    ) AS val
)
WHERE org_alias = '__SYSTEM__' AND role_name = 'BILLING';

-- PROVIDER (already has Practitioner.write, add Organization.write)
UPDATE role_permission_config
SET smart_scopes = (
    SELECT jsonb_agg(DISTINCT val)
    FROM jsonb_array_elements(
        smart_scopes || '["SCOPE_user/Organization.write"]'::jsonb
    ) AS val
)
WHERE org_alias = '__SYSTEM__' AND role_name = 'PROVIDER';

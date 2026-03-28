-- V156: Add AllergyIntolerance read/write SMART scopes to all clinical roles.
-- This was missing from V148 causing 403 errors when accessing the allergies tab.

-- ADMIN / SUPER_ADMIN: full read + write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read", "SCOPE_user/AllergyIntolerance.write"]'::jsonb
WHERE role_name IN ('ADMIN', 'SUPER_ADMIN')
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- PROVIDER: full read + write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read", "SCOPE_user/AllergyIntolerance.write"]'::jsonb
WHERE role_name = 'PROVIDER'
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- NURSE: full read + write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read", "SCOPE_user/AllergyIntolerance.write"]'::jsonb
WHERE role_name = 'NURSE'
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- MA (Medical Assistant): read + write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read", "SCOPE_user/AllergyIntolerance.write"]'::jsonb
WHERE role_name = 'MA'
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- FRONT_DESK: read only
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read"]'::jsonb
WHERE role_name = 'FRONT_DESK'
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- BILLING: read only
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/AllergyIntolerance.read"]'::jsonb
WHERE role_name = 'BILLING'
  AND NOT smart_scopes @> '"SCOPE_user/AllergyIntolerance.read"'::jsonb;

-- PATIENT: read only (patient scope)
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_patient/AllergyIntolerance.read"]'::jsonb
WHERE role_name = 'PATIENT'
  AND NOT smart_scopes @> '"SCOPE_patient/AllergyIntolerance.read"'::jsonb;

-- Add Composition read/write SMART scopes to all roles that need them
-- These scopes are required for encounter-form auto-save (Composition FHIR resource)

-- ADMIN / SUPER_ADMIN roles: add both read and write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/Composition.read", "SCOPE_user/Composition.write"]'::jsonb
WHERE role_name IN ('ADMIN', 'SUPER_ADMIN')
  AND NOT smart_scopes @> '["SCOPE_user/Composition.read"]'::jsonb;

-- PROVIDER role: add both read and write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/Composition.read", "SCOPE_user/Composition.write"]'::jsonb
WHERE role_name = 'PROVIDER'
  AND NOT smart_scopes @> '["SCOPE_user/Composition.read"]'::jsonb;

-- NURSE role: add both read and write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/Composition.read", "SCOPE_user/Composition.write"]'::jsonb
WHERE role_name = 'NURSE'
  AND NOT smart_scopes @> '["SCOPE_user/Composition.read"]'::jsonb;

-- MA role: add both read and write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/Composition.read", "SCOPE_user/Composition.write"]'::jsonb
WHERE role_name = 'MA'
  AND NOT smart_scopes @> '["SCOPE_user/Composition.read"]'::jsonb;

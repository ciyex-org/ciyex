-- V162: Add HealthcareService SMART scopes so Settings > Services shows Add button
-- and backend write is allowed for admin/provider roles.

-- ADMIN / SUPER_ADMIN: full read + write
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/HealthcareService.read","SCOPE_user/HealthcareService.write"]'::jsonb
WHERE role_name IN ('ADMIN', 'SUPER_ADMIN')
  AND NOT smart_scopes @> '"SCOPE_user/HealthcareService.read"'::jsonb;

-- PROVIDER / NURSE / FRONT_DESK: read only
UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_user/HealthcareService.read"]'::jsonb
WHERE role_name IN ('PROVIDER', 'NURSE', 'FRONT_DESK', 'MEDICAL_ASSISTANT', 'BILLER')
  AND NOT smart_scopes @> '"SCOPE_user/HealthcareService.read"'::jsonb;

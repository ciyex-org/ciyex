-- Add patient/Communication.read and patient/Communication.write scopes
-- to all PATIENT role entries in role_permission_config.
-- This allows portal patients to access the messaging/channels API.

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_patient/Communication.read", "SCOPE_patient/Communication.write"]'::jsonb
WHERE role_name = 'PATIENT'
  AND NOT smart_scopes @> '["SCOPE_patient/Communication.read"]'::jsonb;

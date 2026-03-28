-- Add Coverage.read, Claim.read, QuestionnaireResponse.read scopes to PATIENT role
-- so portal billing, insurance, and history pages work correctly.

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_patient/Coverage.read", "SCOPE_patient/Claim.read", "SCOPE_patient/QuestionnaireResponse.read"]'::jsonb
WHERE role_name = 'PATIENT'
  AND NOT smart_scopes @> '["SCOPE_patient/Coverage.read"]'::jsonb;

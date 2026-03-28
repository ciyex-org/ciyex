-- V148: Add smart_scopes JSONB column to role_permission_config
-- Allows org admins to customize SMART on FHIR API-level scopes per role.
-- Static defaults from RolePermissionRegistry are seeded here;
-- SmartScopeResolver falls back to them if no DB row found.

ALTER TABLE role_permission_config
    ADD COLUMN IF NOT EXISTS smart_scopes JSONB DEFAULT '[]';

-- Seed __SYSTEM__ template roles with their current static SMART scopes

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read", "SCOPE_user/Patient.write",
  "SCOPE_user/Appointment.read", "SCOPE_user/Appointment.write",
  "SCOPE_user/Encounter.read", "SCOPE_user/Encounter.write",
  "SCOPE_user/Observation.read", "SCOPE_user/Observation.write",
  "SCOPE_user/Procedure.read", "SCOPE_user/Procedure.write",
  "SCOPE_user/MedicationRequest.read", "SCOPE_user/MedicationRequest.write",
  "SCOPE_user/DiagnosticReport.read", "SCOPE_user/DiagnosticReport.write",
  "SCOPE_user/ServiceRequest.read", "SCOPE_user/ServiceRequest.write",
  "SCOPE_user/CarePlan.read", "SCOPE_user/CarePlan.write",
  "SCOPE_user/Immunization.read", "SCOPE_user/Immunization.write",
  "SCOPE_user/DocumentReference.read", "SCOPE_user/DocumentReference.write",
  "SCOPE_user/Consent.read", "SCOPE_user/Consent.write",
  "SCOPE_user/Task.read", "SCOPE_user/Task.write",
  "SCOPE_user/Claim.read", "SCOPE_user/Claim.write",
  "SCOPE_user/Coverage.read", "SCOPE_user/Coverage.write",
  "SCOPE_user/Practitioner.read", "SCOPE_user/Practitioner.write",
  "SCOPE_user/Organization.read", "SCOPE_user/Organization.write",
  "SCOPE_user/Communication.read", "SCOPE_user/Communication.write"
]'::jsonb
WHERE role_name IN ('ADMIN', 'SUPER_ADMIN') AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read", "SCOPE_user/Patient.write",
  "SCOPE_user/Appointment.read", "SCOPE_user/Appointment.write",
  "SCOPE_user/Encounter.read", "SCOPE_user/Encounter.write",
  "SCOPE_user/Observation.read", "SCOPE_user/Observation.write",
  "SCOPE_user/Procedure.read", "SCOPE_user/Procedure.write",
  "SCOPE_user/MedicationRequest.read", "SCOPE_user/MedicationRequest.write",
  "SCOPE_user/DiagnosticReport.read", "SCOPE_user/DiagnosticReport.write",
  "SCOPE_user/ServiceRequest.read", "SCOPE_user/ServiceRequest.write",
  "SCOPE_user/CarePlan.read", "SCOPE_user/CarePlan.write",
  "SCOPE_user/Immunization.read", "SCOPE_user/Immunization.write",
  "SCOPE_user/DocumentReference.read", "SCOPE_user/DocumentReference.write",
  "SCOPE_user/Consent.read", "SCOPE_user/Consent.write",
  "SCOPE_user/Task.read", "SCOPE_user/Task.write",
  "SCOPE_user/Claim.read",
  "SCOPE_user/Coverage.read",
  "SCOPE_user/Practitioner.read",
  "SCOPE_user/Organization.read",
  "SCOPE_user/Communication.read"
]'::jsonb
WHERE role_name = 'PROVIDER' AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read", "SCOPE_user/Patient.write",
  "SCOPE_user/Appointment.read", "SCOPE_user/Appointment.write",
  "SCOPE_user/Encounter.read", "SCOPE_user/Encounter.write",
  "SCOPE_user/Observation.read", "SCOPE_user/Observation.write",
  "SCOPE_user/Procedure.read",
  "SCOPE_user/MedicationRequest.read",
  "SCOPE_user/DiagnosticReport.read",
  "SCOPE_user/ServiceRequest.read",
  "SCOPE_user/CarePlan.read",
  "SCOPE_user/Immunization.read", "SCOPE_user/Immunization.write",
  "SCOPE_user/DocumentReference.read", "SCOPE_user/DocumentReference.write",
  "SCOPE_user/Consent.read",
  "SCOPE_user/Task.read", "SCOPE_user/Task.write",
  "SCOPE_user/Coverage.read",
  "SCOPE_user/Practitioner.read",
  "SCOPE_user/Organization.read",
  "SCOPE_user/Communication.read", "SCOPE_user/Communication.write"
]'::jsonb
WHERE role_name = 'NURSE' AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read",
  "SCOPE_user/Appointment.read", "SCOPE_user/Appointment.write",
  "SCOPE_user/Encounter.read",
  "SCOPE_user/Observation.read", "SCOPE_user/Observation.write",
  "SCOPE_user/Procedure.read",
  "SCOPE_user/MedicationRequest.read",
  "SCOPE_user/DiagnosticReport.read",
  "SCOPE_user/ServiceRequest.read",
  "SCOPE_user/Immunization.read",
  "SCOPE_user/DocumentReference.read",
  "SCOPE_user/Consent.read",
  "SCOPE_user/Task.read", "SCOPE_user/Task.write",
  "SCOPE_user/Coverage.read",
  "SCOPE_user/Practitioner.read",
  "SCOPE_user/Organization.read"
]'::jsonb
WHERE role_name = 'MA' AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read", "SCOPE_user/Patient.write",
  "SCOPE_user/Appointment.read", "SCOPE_user/Appointment.write",
  "SCOPE_user/Encounter.read",
  "SCOPE_user/Consent.read", "SCOPE_user/Consent.write",
  "SCOPE_user/Coverage.read",
  "SCOPE_user/DocumentReference.read",
  "SCOPE_user/Task.read",
  "SCOPE_user/Practitioner.read",
  "SCOPE_user/Organization.read",
  "SCOPE_user/Communication.read"
]'::jsonb
WHERE role_name = 'FRONT_DESK' AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_user/Patient.read",
  "SCOPE_user/Appointment.read",
  "SCOPE_user/Encounter.read",
  "SCOPE_user/Procedure.read",
  "SCOPE_user/DiagnosticReport.read",
  "SCOPE_user/ServiceRequest.read",
  "SCOPE_user/Claim.read", "SCOPE_user/Claim.write",
  "SCOPE_user/Coverage.read", "SCOPE_user/Coverage.write",
  "SCOPE_user/DocumentReference.read",
  "SCOPE_user/Organization.read"
]'::jsonb
WHERE role_name = 'BILLING' AND smart_scopes = '[]'::jsonb;

UPDATE role_permission_config
SET smart_scopes = '[
  "SCOPE_patient/Patient.read",
  "SCOPE_patient/Appointment.read",
  "SCOPE_patient/Observation.read",
  "SCOPE_patient/MedicationRequest.read",
  "SCOPE_patient/DiagnosticReport.read",
  "SCOPE_patient/DocumentReference.read",
  "SCOPE_patient/CarePlan.read"
]'::jsonb
WHERE role_name = 'PATIENT' AND smart_scopes = '[]'::jsonb;

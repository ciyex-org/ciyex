-- V157: Add missing FHIR resource SMART scopes for Condition, RelatedPerson,
-- Flag, QuestionnaireResponse, Invoice, MeasureReport, Location, ClaimResponse,
-- and ExplanationOfBenefit.
-- These were missing causing 403 errors on medicalproblems, relationships,
-- clinical-alerts, history, payment, statements, issues, report, facility,
-- claim-denials, and era-remittance tabs.

-- ═══════════════════════════════════════════════════════════════════════════
-- ADMIN / SUPER_ADMIN: full read + write for all new resource types
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read", "SCOPE_user/Condition.write",
  "SCOPE_user/RelatedPerson.read", "SCOPE_user/RelatedPerson.write",
  "SCOPE_user/Flag.read", "SCOPE_user/Flag.write",
  "SCOPE_user/QuestionnaireResponse.read", "SCOPE_user/QuestionnaireResponse.write",
  "SCOPE_user/Invoice.read", "SCOPE_user/Invoice.write",
  "SCOPE_user/MeasureReport.read", "SCOPE_user/MeasureReport.write",
  "SCOPE_user/Location.read", "SCOPE_user/Location.write",
  "SCOPE_user/ClaimResponse.read", "SCOPE_user/ClaimResponse.write",
  "SCOPE_user/ExplanationOfBenefit.read", "SCOPE_user/ExplanationOfBenefit.write"
]'::jsonb
WHERE role_name IN ('ADMIN', 'SUPER_ADMIN')
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- PROVIDER: full clinical read/write, read-only for financial
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read", "SCOPE_user/Condition.write",
  "SCOPE_user/RelatedPerson.read",
  "SCOPE_user/Flag.read", "SCOPE_user/Flag.write",
  "SCOPE_user/QuestionnaireResponse.read", "SCOPE_user/QuestionnaireResponse.write",
  "SCOPE_user/Invoice.read",
  "SCOPE_user/MeasureReport.read",
  "SCOPE_user/Location.read",
  "SCOPE_user/ClaimResponse.read",
  "SCOPE_user/ExplanationOfBenefit.read"
]'::jsonb
WHERE role_name = 'PROVIDER'
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- NURSE: clinical read/write, read-only for financial
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read", "SCOPE_user/Condition.write",
  "SCOPE_user/RelatedPerson.read",
  "SCOPE_user/Flag.read", "SCOPE_user/Flag.write",
  "SCOPE_user/QuestionnaireResponse.read", "SCOPE_user/QuestionnaireResponse.write",
  "SCOPE_user/Invoice.read",
  "SCOPE_user/MeasureReport.read",
  "SCOPE_user/Location.read",
  "SCOPE_user/ClaimResponse.read",
  "SCOPE_user/ExplanationOfBenefit.read"
]'::jsonb
WHERE role_name = 'NURSE'
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- MA (Medical Assistant): clinical read-only for new types
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read",
  "SCOPE_user/RelatedPerson.read",
  "SCOPE_user/Flag.read",
  "SCOPE_user/QuestionnaireResponse.read",
  "SCOPE_user/Location.read"
]'::jsonb
WHERE role_name = 'MA'
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- FRONT_DESK: read-only for relevant types
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read",
  "SCOPE_user/RelatedPerson.read",
  "SCOPE_user/Flag.read",
  "SCOPE_user/Invoice.read",
  "SCOPE_user/Location.read"
]'::jsonb
WHERE role_name = 'FRONT_DESK'
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- BILLING: financial read/write, clinical read-only
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '[
  "SCOPE_user/Condition.read",
  "SCOPE_user/Invoice.read", "SCOPE_user/Invoice.write",
  "SCOPE_user/MeasureReport.read",
  "SCOPE_user/ClaimResponse.read", "SCOPE_user/ClaimResponse.write",
  "SCOPE_user/ExplanationOfBenefit.read", "SCOPE_user/ExplanationOfBenefit.write"
]'::jsonb
WHERE role_name = 'BILLING'
  AND NOT smart_scopes @> '"SCOPE_user/Condition.read"'::jsonb;

-- ═══════════════════════════════════════════════════════════════════════════
-- PATIENT: self-access read-only for Condition
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE role_permission_config
SET smart_scopes = smart_scopes || '["SCOPE_patient/Condition.read"]'::jsonb
WHERE role_name = 'PATIENT'
  AND NOT smart_scopes @> '"SCOPE_patient/Condition.read"'::jsonb;

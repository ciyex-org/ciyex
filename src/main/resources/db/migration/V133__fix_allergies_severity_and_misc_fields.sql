-- V133: Fix allergies severity FHIR path and miscellaneous field config fixes
-- 1. Allergies: severity field was mapped to AllergyIntolerance.criticality (low/high/unable-to-assess)
--    but the form options use mild/moderate/severe — correct path is reaction[0].severity
-- 2. Allergies: onset date key was "startDate" but should read from onsetDateTime correctly

-- ─── 1. Fix allergies severity: criticality → reaction[0].severity ───
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"severity","label":"Severity","type":"select","required":false,"colSpan":1,"options":[{"value":"mild","label":"Mild"},{"value":"moderate","label":"Moderate"},{"value":"severe","label":"Severe"}],"fhirMapping":{"resource":"AllergyIntolerance","path":"criticality","type":"code"}',
    '"key":"severity","label":"Severity","type":"select","required":false,"colSpan":1,"options":[{"value":"mild","label":"Mild"},{"value":"moderate","label":"Moderate"},{"value":"severe","label":"Severe"}],"fhirMapping":{"resource":"AllergyIntolerance","path":"reaction[0].severity","type":"code"}'
)::jsonb,
updated_at = now()
WHERE tab_key = 'allergies'
  AND field_config::text LIKE '%"path":"criticality","type":"code"%';

-- ─── 2. Fix ClaimResponse request field: ensure reference type is used (not text) ───
-- Change request.reference from type=text to type=reference to enable proper Claim lookup
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"request","label":"Original Claim Ref","type":"text","placeholder":"e.g. Claim/123","fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"string"}',
    '"key":"request","label":"Original Claim","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/fhir/Claim","displayField":"id","valueField":"id","searchable":false},"fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"reference"}'
)::jsonb,
updated_at = now()
WHERE tab_key IN ('claim-denials', 'claim-submissions')
  AND field_config::text LIKE '%"key":"request","label":"Original Claim Ref","type":"text"%';

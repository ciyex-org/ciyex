-- Fix providerType fhirMapping: add system and use coding[1] to avoid conflict with taxonomyCode (coding[0])
-- PostgreSQL stores jsonb keys in alphabetical order, so we match that order in REPLACE
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"fhirMapping": {"path": "qualification[0].code.coding[0].code", "resource": "Practitioner", "type": "code"}',
    '"fhirMapping": {"path": "qualification[0].code.coding[1].code", "resource": "Practitioner", "system": "http://terminology.hl7.org/CodeSystem/practitioner-role", "type": "code"}'
)::jsonb
WHERE tab_key = 'providers'
  AND field_config::text LIKE '%professionalDetails.providerType%';

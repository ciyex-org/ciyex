-- Add patientRef field mapping to encounters tab so GenericFhirResourceService
-- extracts the patient reference from FHIR Encounter.subject.reference.
-- enrichEncounterFields() then parses "Patient/12345" → patientId = 12345.

UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config,
    '{sections,0,fields}',
    (field_config #> '{sections,0,fields}') || '[{"key":"patientRef","label":"Patient","type":"hidden","fhirMapping":{"resource":"Encounter","path":"subject.reference","type":"reference"}}]'::jsonb
),
    updated_at = now()
WHERE tab_key = 'encounters'
  AND practice_type_code = '*'
  AND org_id = '*';

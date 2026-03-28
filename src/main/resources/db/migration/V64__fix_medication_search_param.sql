-- Fix MedicationRequest search parameter: FHIR server uses 'subject' not 'patient'

-- Fix medications tab
UPDATE tab_field_config
SET fhir_resources = '[{"type": "MedicationRequest", "patientSearchParam": "subject"}]'::jsonb
WHERE tab_key = 'medications';

-- Fix issues tab: MedicationRequest needs 'subject' not 'patient'
UPDATE tab_field_config
SET fhir_resources = '[{"type": "Condition", "patientSearchParam": "subject"}, {"type": "AllergyIntolerance", "patientSearchParam": "patient"}, {"type": "MedicationRequest", "patientSearchParam": "subject"}]'::jsonb
WHERE tab_key = 'issues';

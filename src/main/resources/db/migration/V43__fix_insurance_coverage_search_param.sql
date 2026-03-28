-- V43: Fix insurance-coverage tab - wrong field name in fhir_resources
-- Was "searchParam" (unrecognized), should be "patientSearchParam" (what the parser expects)
-- This caused Coverage to default to search param "subject" which is invalid for Coverage resource

UPDATE tab_field_config
SET fhir_resources = '[{"type": "Coverage", "patientSearchParam": "beneficiary"}]',
    version = version + 1,
    updated_at = now()
WHERE tab_key = 'insurance-coverage';

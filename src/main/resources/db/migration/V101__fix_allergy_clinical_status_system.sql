-- V100: Fix AllergyIntolerance clinicalStatus FHIR path — add required system URI
-- Without the system, the Coding is technically invalid per FHIR R4 spec.
-- The clinicalStatus binding requires system = http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical

UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path":"clinicalStatus.coding[0].code","type":"code"',
    '"path":"clinicalStatus.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"'
)::jsonb
WHERE tab_key = 'allergies'
  AND field_config::text LIKE '%clinicalStatus.coding[0].code%'
  AND field_config::text NOT LIKE '%allergyintolerance-clinical%';

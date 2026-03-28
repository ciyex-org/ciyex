-- Fix genderIdentity extension: FHIR R4 patient-genderIdentity uses valueCodeableConcept, not valueCode
-- Also fix pronouns extension type (individual-pronouns uses valueCodeableConcept in R4)

UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path": "extension[url=http://hl7.org/fhir/StructureDefinition/patient-genderIdentity].valueCode"',
    '"path": "extension[url=http://hl7.org/fhir/StructureDefinition/patient-genderIdentity].valueCodeableConcept"'
)::jsonb
WHERE field_config::text LIKE '%patient-genderIdentity%'
  AND field_config::text LIKE '%valueCode%';

-- Also fix pronouns extension (uses valueCodeableConcept not valueCode)
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path": "extension[url=http://hl7.org/fhir/StructureDefinition/individual-pronouns].valueCode"',
    '"path": "extension[url=http://hl7.org/fhir/StructureDefinition/individual-pronouns].valueCodeableConcept"'
)::jsonb
WHERE field_config::text LIKE '%individual-pronouns%'
  AND field_config::text LIKE '%valueCode%';

-- Fix race extension (us-core-race uses valueCodeableConcept not valueCode)
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-race].valueCode"',
    '"path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-race].valueCodeableConcept"'
)::jsonb
WHERE field_config::text LIKE '%us-core-race%'
  AND field_config::text LIKE '%valueCode%';

-- Fix ethnicity extension (us-core-ethnicity uses valueCodeableConcept not valueCode)
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity].valueCode"',
    '"path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity].valueCodeableConcept"'
)::jsonb
WHERE field_config::text LIKE '%us-core-ethnicity%'
  AND field_config::text LIKE '%valueCode%';

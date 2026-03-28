-- V152: Remove fhirMapping from appointment room field.
-- The FHIR server rejects unknown extension URLs.  Room data is already
-- persisted through the generic form-data extension, so a dedicated
-- FHIR extension mapping is unnecessary.

UPDATE tab_field_config SET
  field_config = REPLACE(
    field_config::text,
    '"fhirMapping":{"path":"extension.where(url=''http://ciyex.com/fhir/StructureDefinition/appointment-room'').valueString","type":"string","resource":"Appointment"}',
    '"fhirMapping":null'
  )::jsonb,
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config::text LIKE '%appointment-room%';

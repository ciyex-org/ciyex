-- V151: Fix appointment room extension URL to be absolute (FHIR requirement)
-- V150 had incorrect SQL quoting and did not match. This uses correct escaping.
-- The JSON stores: extension.where(url='room').valueString  (single quotes around room)

UPDATE tab_field_config SET
  field_config = REPLACE(
    field_config::text,
    'extension.where(url=''room'').valueString',
    'extension.where(url=''http://ciyex.com/fhir/StructureDefinition/appointment-room'').valueString'
  )::jsonb,
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config::text LIKE '%extension.where(url=''room'')%';

-- V150: Fix appointment room extension URL to be absolute (FHIR requirement)
-- The 'room' extension was using a relative URL which causes HTTP 422 on create.

UPDATE tab_field_config SET
  field_config = REPLACE(
    field_config::text,
    'extension.where(url=''''room'''').valueString',
    'extension.where(url=''''http://ciyex.com/fhir/StructureDefinition/appointment-room'''').valueString'
  )::jsonb,
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config::text LIKE '%extension.where(url=''''room'''')%';

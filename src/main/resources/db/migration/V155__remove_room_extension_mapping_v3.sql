-- V155: Remove fhirMapping from appointment room field (v3).
-- V152 and V154 both failed because JSONB ::text uses "key": "room" (with space)
-- but the LIKE pattern used "key":"room" (no space).

UPDATE tab_field_config SET
  field_config = jsonb_set(
    field_config,
    '{sections,0,fields,2,fhirMapping}',
    'null'::jsonb
  ),
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config #>> '{sections,0,fields,2,key}' = 'room'
  AND field_config #>> '{sections,0,fields,2,fhirMapping,path}' LIKE '%appointment-room%';

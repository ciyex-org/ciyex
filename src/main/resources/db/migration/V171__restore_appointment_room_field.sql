-- V171: Restore room field to appointments tab_field_config.
-- V169 replaced the entire config without the room field, breaking room assignment.
-- Add room as a form-data-only field (no fhirMapping) in the first section.

UPDATE tab_field_config SET
  field_config = jsonb_set(
    field_config,
    '{sections,0,fields}',
    (field_config #> '{sections,0,fields}') || '[{"key":"room","label":"Room","type":"select","required":false,"colSpan":1,"showInTable":true,"options":["Exam 1","Exam 2","Exam 3","Exam 4","Lab","Procedure Room","Triage"]}]'::jsonb
  ),
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config #>> '{sections,0,fields}' IS NOT NULL
  AND NOT (field_config::text LIKE '%"key":"room"%' OR field_config::text LIKE '%"key": "room"%');

-- V154: Remove fhirMapping from appointment room field (v2).
-- V152 failed because the JSON field order in the REPLACE pattern didn't match.
-- This uses jsonb_set with array path to null out the mapping reliably.

UPDATE tab_field_config SET
  field_config = (
    SELECT jsonb_set(
      field_config,
      path,
      'null'::jsonb
    )
    FROM (
      SELECT
        ARRAY['sections', (s_idx - 1)::text, 'fields', (f_idx - 1)::text, 'fhirMapping'] AS path
      FROM jsonb_array_elements(field_config->'sections') WITH ORDINALITY AS s(sec, s_idx),
           jsonb_array_elements(s.sec->'fields') WITH ORDINALITY AS f(fld, f_idx)
      WHERE f.fld->>'key' = 'room'
      LIMIT 1
    ) sub
  ),
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments'
  AND field_config::text LIKE '%"key":"room"%'
  AND field_config::text LIKE '%appointment-room%';

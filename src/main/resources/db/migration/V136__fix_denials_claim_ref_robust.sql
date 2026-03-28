-- Robustly fix claim-denials: change fhirMapping type from reference to string
-- and field type from lookup to text. Previous V134 may not have matched due to
-- JSONB key reordering. This uses broader matching.

UPDATE tab_field_config
SET field_config = REPLACE(
    REPLACE(
        field_config::text,
        '"type":"reference"',
        '"type":"string"'
    ),
    '"type":"lookup"',
    '"type":"text"'
)::jsonb,
updated_at = now()
WHERE tab_key IN ('claim-denials', 'claim-submissions')
  AND field_config::text LIKE '%request.reference%';

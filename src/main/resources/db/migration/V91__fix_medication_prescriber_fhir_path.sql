-- Fix medications prescriber FHIR path: requester.display → requester.reference
-- The .display path extracts display text which cannot be resolved to a provider name.
-- The .reference path extracts the reference ID (e.g., Practitioner/123) which enables display resolution.
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path":"requester.display","type":"reference"',
    '"path":"requester.reference","type":"reference"'
)::jsonb
WHERE tab_key = 'medications'
  AND field_config::text LIKE '%requester.display%';

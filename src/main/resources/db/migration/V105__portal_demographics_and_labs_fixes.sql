-- V105: Fix portal demographics + labs tab config (retry of V104 with correct type casts)
-- field_config is jsonb; REPLACE() requires text cast.

-- Fix portal-demographics: use simple ciyex extension URLs for race/ethnicity
UPDATE tab_field_config
SET field_config = REPLACE(
    REPLACE(
        field_config::text,
        'extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-race].valueCode',
        'extension[url=http://ciyex.org/fhir/ext/race].valueCode'
    ),
    'extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity].valueCode',
    'extension[url=http://ciyex.org/fhir/ext/ethnicity].valueCode'
)::jsonb,
updated_at = now()
WHERE tab_key = 'portal-demographics';

-- Add performer display to labs tab config (only if not already present)
UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config::jsonb,
    '{sections,0,fields}',
    (field_config::jsonb -> 'sections' -> 0 -> 'fields')::jsonb ||
    '[{"key":"performer","label":"Provider","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"performer[0].display","type":"string"}}]'::jsonb
)::jsonb,
updated_at = now()
WHERE tab_key = 'labs'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND NOT (field_config::text LIKE '%"key":"performer"%');

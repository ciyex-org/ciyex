-- V132: Fix labs performer field from plain text to lookup, enabling provider search
-- Also ensure prescriber field in medications uses lookup for proper editing

-- ─── 1. Labs: change performer from type=text to type=lookup ───
-- Find the performer field in labs fieldConfig sections and update its type to lookup
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"performer","label":"Provider","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"performer[0].display","type":"string"}',
    '"key":"performer","label":"Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"DiagnosticReport","path":"performer[0].reference","type":"reference"}'
)::jsonb,
updated_at = now()
WHERE tab_key = 'labs'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND field_config::text LIKE '%"key":"performer","label":"Provider","type":"text"%';

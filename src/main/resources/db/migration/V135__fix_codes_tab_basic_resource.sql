-- Fix codes tab: Change FHIR resource type from CodeSystem to Basic
-- CodeSystem cannot be partitioned in HAPI FHIR, causing HTTP 422.
-- Basic resource supports partitioning and is used by GlobalCodeService.

-- 1. Change fhir_resources from CodeSystem/ValueSet to Basic
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Basic"}]'::jsonb,
    updated_at = now()
WHERE tab_key = 'codes'
  AND fhir_resources::text LIKE '%CodeSystem%';

-- 2. Change all fhirMapping.resource references from CodeSystem to Basic in field_config
UPDATE tab_field_config
SET field_config = REPLACE(field_config::text, '"resource":"CodeSystem"', '"resource":"Basic"')::jsonb,
    updated_at = now()
WHERE tab_key = 'codes'
  AND field_config::text LIKE '%"resource":"CodeSystem"%';

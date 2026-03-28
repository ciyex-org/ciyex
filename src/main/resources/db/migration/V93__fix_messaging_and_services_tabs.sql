-- V93: Fix messaging and services tab configurations
--
-- 1. Messaging: Add showInTable to key fields so they appear in list view
-- 2. Services (healthcareservices): Remove from patient chart scope
--    HealthcareService is NOT a patient-scoped FHIR resource. It represents
--    services offered by an org/location. Having patientSearchParam="" causes
--    broken FHIR queries and data that "vanishes on refresh".

-- 1a. Add showInTable to messaging subject field
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"subject","type":"text","label":"Subject","colSpan":3',
    '"key":"subject","type":"text","label":"Subject","colSpan":3,"showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'messaging'
  AND field_config::text LIKE '%"key":"subject"%'
  AND field_config::text NOT LIKE '%"key":"subject"%showInTable%';

-- 1b. Add showInTable to messaging sent field
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"sent","type":"datetime","label":"Sent Date"',
    '"key":"sent","type":"datetime","label":"Sent Date","showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'messaging'
  AND field_config::text LIKE '%"key":"sent"%'
  AND field_config::text NOT LIKE '%"key":"sent"%showInTable%';

-- 1c. Add showInTable to messaging sender field
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"sender","type":"lookup","label":"From"',
    '"key":"sender","type":"lookup","label":"From","showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'messaging'
  AND field_config::text LIKE '%"key":"sender"%'
  AND field_config::text NOT LIKE '%"key":"sender"%showInTable%';

-- 1d. Add showInTable to messaging recipient field
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"recipient","type":"lookup","label":"To"',
    '"key":"recipient","type":"lookup","label":"To","showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'messaging'
  AND field_config::text LIKE '%"key":"recipient"%'
  AND field_config::text NOT LIKE '%"key":"recipient"%showInTable%';

-- 1e. Add showInTable to messaging status field
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"status","type":"select","label":"Status","colSpan":1',
    '"key":"status","type":"select","label":"Status","colSpan":1,"showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'messaging'
  AND field_config::text LIKE '%"key":"status","type":"select"%'
  AND field_config::text NOT LIKE '%"key":"status"%showInTable%';

-- 2. Remove 'healthcareservices' from patient chart tabs since HealthcareService
--    is not patient-scoped. The 'services' tab (Settings category) already exists
--    for managing org-level healthcare services.
DELETE FROM tab_field_config WHERE tab_key = 'healthcareservices';

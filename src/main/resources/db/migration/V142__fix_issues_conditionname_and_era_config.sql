-- V142: Fix issues conditionName FHIR path

-- Fix issues tab: change conditionName fhirMapping from code.text to code.coding[0].display
-- Seeded Condition resources have code.coding[0].display but NOT code.text
UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config,
    '{sections,0,fields,0,fhirMapping,path}',
    '"code.coding[0].display"'
)
WHERE tab_key = 'issues' AND org_id = '*';

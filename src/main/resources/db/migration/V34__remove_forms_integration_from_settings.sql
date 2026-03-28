-- Remove Forms and Integration from the Settings sidebar
UPDATE tab_field_config
SET category = NULL, visible = false
WHERE tab_key IN ('forms', 'integration')
AND org_id = '*';

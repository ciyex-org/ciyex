-- Change Practice Type field from 'select' to 'combobox' to allow custom practice types
UPDATE tab_field_config
SET field_config = replace(field_config::text, '"type": "select", "label": "Practice Type"', '"type": "combobox", "label": "Practice Type"')::jsonb
WHERE tab_key = 'practice' AND org_id = '*';

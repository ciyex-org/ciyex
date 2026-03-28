-- V21: Add category_position to control category display order
-- Without this, categories sort alphabetically instead of the intended order.

ALTER TABLE tab_field_config
  ADD COLUMN IF NOT EXISTS category_position INT DEFAULT 0;

-- Overview = 0, General = 1, Encounters = 2, Clinical = 3, Claims = 4, Financial = 5, Other = 6
UPDATE tab_field_config SET category_position = 0 WHERE category = 'Overview';
UPDATE tab_field_config SET category_position = 1 WHERE category = 'General';
UPDATE tab_field_config SET category_position = 2 WHERE category = 'Encounters';
UPDATE tab_field_config SET category_position = 3 WHERE category = 'Clinical';
UPDATE tab_field_config SET category_position = 4 WHERE category = 'Claims';
UPDATE tab_field_config SET category_position = 5 WHERE category = 'Financial';
UPDATE tab_field_config SET category_position = 6 WHERE category = 'Other';

-- Update the layout index to include category_position
DROP INDEX IF EXISTS idx_tfc_layout;
CREATE INDEX idx_tfc_layout ON tab_field_config (org_id, practice_type_code, category_position, position);

-- V100: Fix System menu item icon
-- System and Settings both used 'Settings' (gear) icon — change System to 'Monitor'
UPDATE menu_item
SET icon = 'Monitor'
WHERE item_key = 'system'
  AND parent_id IS NULL;

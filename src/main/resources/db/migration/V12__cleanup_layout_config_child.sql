-- Fix: "Layout Settings" is parent, "Chart" is child submenu
-- V11 incorrectly collapsed them into one item

-- Revert parent back to "Layout Settings" (no direct path)
UPDATE menu_item
SET label       = 'Layout Settings',
    item_key    = 'settings-layout',
    icon        = 'LayoutDashboard',
    screen_slug = NULL
WHERE item_key = 'settings-chart';

-- Remove orphaned old child (renamed by V2)
DELETE FROM menu_item WHERE item_key = 'settings-patient-chart-layout';

-- Insert "Chart" as child of each "Layout Settings" parent
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position)
SELECT
    gen_random_uuid(),
    mi.menu_id,
    mi.id,
    'settings-chart',
    'Chart',
    'LayoutGrid',
    '/settings/layout-settings',
    0
FROM menu_item mi
WHERE mi.item_key = 'settings-layout'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item child
    WHERE child.parent_id = mi.id AND child.item_key = 'settings-chart'
  );

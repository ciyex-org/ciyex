-- Rename "Layout Settings" parent to "Chart" and collapse into direct link
-- Column names: item_key, label, icon, screen_slug

-- Update "Layout Settings" parent: give it the path directly, rename to "Chart"
UPDATE menu_item
SET label       = 'Chart',
    item_key    = 'settings-chart',
    icon        = 'LayoutGrid',
    screen_slug = '/settings/layout-settings'
WHERE item_key = 'settings-layout';

-- Delete the "Layout Configuration" child items (now redundant)
DELETE FROM menu_item WHERE item_key = 'settings-layout-config';

-- Also update the original item from V1 if it still exists
UPDATE menu_item
SET label       = 'Chart',
    item_key    = 'settings-chart',
    screen_slug = '/settings/layout-settings'
WHERE item_key = 'settings-tab-config';

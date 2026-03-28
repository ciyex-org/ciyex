-- Fix settings-chart: rename from "Layout Settings" to "Chart" and set screen_slug
UPDATE menu_item
SET label = 'Chart',
    screen_slug = '/settings/layout-settings',
    updated_at = now()
WHERE item_key = 'settings-chart';

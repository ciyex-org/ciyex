-- V85: Remove inventory and labs sub-menu items from sidebar
-- Both pages now have built-in sub-sidebar navigation (like Reports).

-- Delete all inventory sub-items across all practice types
DELETE FROM menu_item WHERE item_key IN (
    'inv-dashboard',
    'inv-inventory',
    'inv-orders',
    'inv-records',
    'inv-suppliers',
    'inv-maintenance',
    'inv-settings'
);

-- Delete all labs sub-items across all practice types
DELETE FROM menu_item WHERE item_key IN (
    'labs-orders',
    'labs-results'
);

-- Ensure parent "Inventory" items point to /inventory-management
UPDATE menu_item
SET screen_slug = '/inventory-management'
WHERE item_key = 'inventory' AND (screen_slug IS NULL OR screen_slug = '');

-- Ensure parent "Labs" items point to /labs
UPDATE menu_item
SET screen_slug = '/labs'
WHERE item_key = 'labs' AND (screen_slug IS NULL OR screen_slug = '');

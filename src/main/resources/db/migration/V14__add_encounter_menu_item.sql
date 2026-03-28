-- Add "Encounter" as child of each "Layout Settings" parent (position 2)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position)
SELECT
    gen_random_uuid(),
    mi.menu_id,
    mi.id,
    'settings-encounter',
    'Encounter',
    'ClipboardList',
    '/settings/encounter-settings',
    2
FROM menu_item mi
WHERE mi.item_key = 'settings-layout'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item child
    WHERE child.parent_id = mi.id AND child.item_key = 'settings-encounter'
  );

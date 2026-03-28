-- V47: Add "Ciyex Hub" as top-level menu item in all ehr-sidebar menus
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position, created_at, updated_at)
SELECT
    gen_random_uuid(),
    m.id,
    NULL,
    'ciyex-hub',
    'Ciyex Hub',
    'Store',
    '/hub',
    90,
    now(), now()
FROM menu m
WHERE m.code = 'ehr-sidebar'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item existing
    WHERE existing.item_key = 'ciyex-hub'
      AND existing.menu_id = m.id
  );

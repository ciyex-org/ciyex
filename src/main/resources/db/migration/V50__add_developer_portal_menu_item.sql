-- V50: Add "Developer Portal" as top-level menu item in all ehr-sidebar menus
-- Uses icon "Code2" (Lucide). Position 91 (right after Ciyex Hub at 90).
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position, created_at, updated_at)
SELECT
    gen_random_uuid(),
    m.id,
    NULL,
    'developer-portal',
    'Developer Portal',
    'Code2',
    '/developer',
    91,
    now(), now()
FROM menu m
WHERE m.code = 'ehr-sidebar'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item existing
    WHERE existing.item_key = 'developer-portal'
      AND existing.menu_id = m.id
  );

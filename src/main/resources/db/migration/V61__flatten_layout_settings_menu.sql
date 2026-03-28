-- Remove Layout Settings submenu items and make it a direct link.
-- The sidebar in settings/layout.tsx now handles sub-navigation (Chart, Menu, Encounter, Settings).

-- Step 1: Set screen_slug on all "Layout Settings" parent items so they navigate directly
UPDATE menu_item
SET screen_slug = '/settings/layout-settings'
WHERE label = 'Layout Settings'
  AND parent_id IS NULL;

-- Step 2: Delete all children of "Layout Settings" parents
DELETE FROM menu_item
WHERE parent_id IN (
    SELECT id FROM menu_item WHERE label = 'Layout Settings' AND parent_id IS NULL
);

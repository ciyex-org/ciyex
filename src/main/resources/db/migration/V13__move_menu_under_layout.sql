-- Move "Menu Configuration" under "Layout Settings" and rename to "Menu"
UPDATE menu_item
SET label     = 'Menu',
    item_key  = 'settings-menu',
    position  = 1,
    parent_id = parent_layout.id
FROM menu_item parent_layout
WHERE menu_item.item_key = 'settings-menu-config'
  AND parent_layout.item_key = 'settings-layout'
  AND parent_layout.menu_id = menu_item.menu_id;

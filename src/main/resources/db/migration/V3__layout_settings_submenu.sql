-- Create "Layout Settings" parent under Settings, move "Patient Chart Layout" under it

-- Insert "Layout Settings" as a child of Settings (b0...07), position 13 (where tab-config was)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position)
VALUES ('c0000000-0000-0000-0000-000000000060', 'a0000000-0000-0000-0000-000000000001',
        'b0000000-0000-0000-0000-000000000007', 'settings-layout', 'Layout Settings', 'LayoutDashboard', NULL, 13);

-- Move "Patient Chart Layout" (c0...43) to be a child of "Layout Settings" (c0...60)
UPDATE menu_item
SET parent_id = 'c0000000-0000-0000-0000-000000000060',
    position = 0,
    updated_at = now()
WHERE id = 'c0000000-0000-0000-0000-000000000043';

-- Bump "Menu Configuration" position to 14
UPDATE menu_item
SET position = 14, updated_at = now()
WHERE id = 'c0000000-0000-0000-0000-000000000044';

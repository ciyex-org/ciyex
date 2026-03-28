-- Rename "Patient Chart Layout" to "Layout Configuration" in menu
UPDATE menu_item
SET label = 'Layout Configuration',
    updated_at = now()
WHERE id = 'c0000000-0000-0000-0000-000000000043';

-- V96: Revert Reports to flat sidebar link
--
-- AI Usage is now embedded inside the Reports page as a tab,
-- so the "reports-group" parent with children is no longer needed.
-- Restore "reports" as a single flat link at position 25.

DO $$
DECLARE
    v_menu      UUID := 'a0000000-0000-0000-0000-000000000001';
    v_group_id  UUID;
BEGIN
    -- Find the reports-group parent
    SELECT id INTO v_group_id FROM menu_item
    WHERE item_key = 'reports-group' AND menu_id = v_menu AND parent_id IS NULL;

    IF v_group_id IS NOT NULL THEN
        -- Delete the child items (reports-main and admin-ai-usage)
        DELETE FROM menu_item WHERE parent_id = v_group_id AND menu_id = v_menu;

        -- Convert the group back to a flat link
        UPDATE menu_item
        SET item_key = 'reports',
            screen_slug = '/reports',
            position = 25
        WHERE id = v_group_id;
    END IF;
END $$;

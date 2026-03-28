-- V94: Administration menu group + ask-ciya service URL for app proxy
--
-- 1. Add "Administration" parent menu item with Users, Roles, AI Usage children
-- 2. Add service_url to ask-ciya app installations for app proxy routing

-- ── 1. Administration parent menu item with children ──
DO $$
DECLARE
    v_parent_id UUID;
BEGIN
    -- Insert parent only if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM menu_item
        WHERE item_key = 'administration'
          AND menu_id = 'a0000000-0000-0000-0000-000000000001'
    ) THEN
        INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
        VALUES ('a0000000-0000-0000-0000-000000000001', NULL, 'administration', 'Administration', 'ShieldAlert', NULL, 37)
        RETURNING id INTO v_parent_id;

        -- Child: Users
        INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
        VALUES ('a0000000-0000-0000-0000-000000000001', v_parent_id, 'admin-users', 'Users', 'Users', '/settings/user-management', 0);

        -- Child: Roles & Permissions
        INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
        VALUES ('a0000000-0000-0000-0000-000000000001', v_parent_id, 'admin-roles', 'Roles & Permissions', 'Shield', '/settings/roles-permissions', 1);

        -- Child: AI Usage
        INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
        VALUES ('a0000000-0000-0000-0000-000000000001', v_parent_id, 'admin-ai-usage', 'AI Usage', 'Bot', '/settings/ai-usage', 2);
    END IF;
END $$;

-- ── 2. Add service_url to ask-ciya app installations for app proxy routing ──
UPDATE app_installations
SET config = config || '{"service_url": "http://ask-ciya.ask-ciya.svc.cluster.local:8080"}'::jsonb,
    updated_at = now()
WHERE app_slug = 'ask-ciya'
  AND (config ->> 'service_url') IS NULL;

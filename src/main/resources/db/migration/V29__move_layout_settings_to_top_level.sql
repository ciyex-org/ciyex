-- Restructure: Layout Settings becomes top-level, Settings is a leaf with side menu.
-- Under Layout Settings: keep Chart, Menu, Encounter + add "Settings" for tab field config.
-- Remove individual config pages (Providers, Insurance, etc.) from Layout Settings sidebar.

-- 1. Make Layout Settings a top-level item (above Settings)
UPDATE menu_item
SET parent_id = NULL,
    position = 5,
    updated_at = now()
WHERE item_key = 'settings-layout';

-- 2. Make Settings a leaf item (opens page with data side menu, no sidebar children)
UPDATE menu_item
SET position = 6,
    screen_slug = '/settings',
    updated_at = now()
WHERE item_key = 'settings'
  AND parent_id IS NULL;

-- 3. Remove data-* items (rendered inside the Settings page, not sidebar)
DELETE FROM menu_item WHERE item_key LIKE 'data-%';

-- 4. Remove individual config pages from Layout Settings sidebar
-- (Providers, Referral Providers, etc. — they'll be accessed via Settings tab config)
DELETE FROM menu_item WHERE item_key IN (
    'settings-providers',
    'settings-referral-providers',
    'settings-referral-practices',
    'settings-insurance',
    'settings-documents',
    'settings-template-documents',
    'settings-codes',
    'settings-integration',
    'settings-services',
    'settings-billing',
    'settings-forms',
    'settings-facilities',
    'settings-practice',
    'settings-fee-schedules'
);

-- 5. Add "Settings" as a child of Layout Settings — opens tab field config for settings pages
INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position, created_at, updated_at)
SELECT
    layout.menu_id,
    layout.id,
    'layout-settings-config',
    'Settings',
    'Settings',
    '/settings/layout-settings/config/settings',
    3,
    now(), now()
FROM menu_item layout
WHERE layout.item_key = 'settings-layout'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item existing
    WHERE existing.item_key = 'layout-settings-config'
      AND existing.menu_id = layout.menu_id
  );

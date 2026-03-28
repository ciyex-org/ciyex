-- Move settings pages under Layout Settings and update paths to dynamic routes
-- Affects: default menu, dentistry, psychiatry, physical-therapy menus

-- Step 1: Update screen_slug to use dynamic route /settings/p/{pageKey} across ALL menus
UPDATE menu_item SET screen_slug = '/settings/p/providers',           updated_at = now() WHERE item_key = 'settings-providers';
UPDATE menu_item SET screen_slug = '/settings/p/referral-providers',  updated_at = now() WHERE item_key = 'settings-referral-providers';
UPDATE menu_item SET screen_slug = '/settings/p/referral-practices',  updated_at = now() WHERE item_key = 'settings-referral-practices';
UPDATE menu_item SET screen_slug = '/settings/p/insurance',           updated_at = now() WHERE item_key = 'settings-insurance';
UPDATE menu_item SET screen_slug = '/settings/p/documents',           updated_at = now() WHERE item_key = 'settings-documents';
UPDATE menu_item SET screen_slug = '/settings/p/template-documents',  updated_at = now() WHERE item_key = 'settings-template-documents';
UPDATE menu_item SET screen_slug = '/settings/p/codes',               updated_at = now() WHERE item_key = 'settings-codes';
UPDATE menu_item SET screen_slug = '/settings/p/integration',         updated_at = now() WHERE item_key = 'settings-integration';
UPDATE menu_item SET screen_slug = '/settings/p/services',            updated_at = now() WHERE item_key = 'settings-services';
UPDATE menu_item SET screen_slug = '/settings/p/billing',             updated_at = now() WHERE item_key = 'settings-billing';
UPDATE menu_item SET screen_slug = '/settings/p/facilities',          updated_at = now() WHERE item_key = 'settings-facilities';
UPDATE menu_item SET screen_slug = '/settings/p/practice',            updated_at = now() WHERE item_key = 'settings-practice';
UPDATE menu_item SET screen_slug = '/settings/p/fee-schedules',       updated_at = now() WHERE item_key = 'settings-fee-schedules';

-- Forms: make it a direct link (remove sub-items)
UPDATE menu_item SET screen_slug = '/settings/p/forms', updated_at = now() WHERE item_key = 'settings-forms';
DELETE FROM menu_item WHERE item_key IN ('settings-forms-lists', 'settings-forms-admin');

-- Step 2: Move settings page items under Layout Settings parent
-- Layout Settings item_key: 'settings-layout' (practice-type menus) or 'settings-chart' (default menu after V11)
UPDATE menu_item child
SET parent_id = layout_parent.id,
    updated_at = now()
FROM menu_item layout_parent
WHERE child.item_key IN (
    'settings-providers', 'settings-referral-providers', 'settings-referral-practices',
    'settings-insurance', 'settings-documents', 'settings-template-documents',
    'settings-codes', 'settings-integration', 'settings-services', 'settings-billing',
    'settings-forms', 'settings-facilities', 'settings-practice', 'settings-fee-schedules'
)
AND layout_parent.item_key IN ('settings-layout', 'settings-chart')
AND layout_parent.menu_id = child.menu_id;

-- Step 3: Re-number positions under Layout Settings (Menu=0, Chart=1, Encounter=2, then settings pages)
-- Keep Menu, Chart, Encounter at top, then add settings pages starting at position 3
UPDATE menu_item SET position = 3,  updated_at = now() WHERE item_key = 'settings-providers'          AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 4,  updated_at = now() WHERE item_key = 'settings-referral-providers' AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 5,  updated_at = now() WHERE item_key = 'settings-referral-practices' AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 6,  updated_at = now() WHERE item_key = 'settings-insurance'          AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 7,  updated_at = now() WHERE item_key = 'settings-documents'          AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 8,  updated_at = now() WHERE item_key = 'settings-template-documents' AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 9,  updated_at = now() WHERE item_key = 'settings-codes'              AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 10, updated_at = now() WHERE item_key = 'settings-integration'        AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 11, updated_at = now() WHERE item_key = 'settings-services'           AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 12, updated_at = now() WHERE item_key = 'settings-billing'            AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 13, updated_at = now() WHERE item_key = 'settings-forms'              AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 14, updated_at = now() WHERE item_key = 'settings-facilities'         AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 15, updated_at = now() WHERE item_key = 'settings-practice'           AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));
UPDATE menu_item SET position = 16, updated_at = now() WHERE item_key = 'settings-fee-schedules'      AND parent_id IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));

-- Step 4: Ensure Layout Settings parent has no direct screen_slug (it's a grouping node)
UPDATE menu_item
SET screen_slug = NULL,
    label = 'Layout Settings',
    updated_at = now()
WHERE item_key IN ('settings-layout', 'settings-chart')
  AND screen_slug IS NOT NULL;

-- Step 5: Delete the old "Menu Configuration" items that were moved to Layout Settings in V13
-- (they should already be children of Layout Settings, but clean up any orphans)
DELETE FROM menu_item
WHERE item_key = 'settings-menu-config'
  AND parent_id NOT IN (SELECT id FROM menu_item WHERE item_key IN ('settings-layout', 'settings-chart'));

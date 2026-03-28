-- Restructure settings pages:
-- 1. Layout Settings children → config page URLs (/settings/layout-settings/config/{key})
-- 2. Add data page items directly under Settings (/settings/p/{key})

-- Step 1: Update screen_slug for items under Layout Settings to config page URLs
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/providers',           updated_at = now() WHERE item_key = 'settings-providers';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/referral-providers',  updated_at = now() WHERE item_key = 'settings-referral-providers';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/referral-practices',  updated_at = now() WHERE item_key = 'settings-referral-practices';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/insurance',           updated_at = now() WHERE item_key = 'settings-insurance';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/documents',           updated_at = now() WHERE item_key = 'settings-documents';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/template-documents',  updated_at = now() WHERE item_key = 'settings-template-documents';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/codes',               updated_at = now() WHERE item_key = 'settings-codes';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/integration',         updated_at = now() WHERE item_key = 'settings-integration';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/services',            updated_at = now() WHERE item_key = 'settings-services';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/billing',             updated_at = now() WHERE item_key = 'settings-billing';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/forms',               updated_at = now() WHERE item_key = 'settings-forms';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/facilities',          updated_at = now() WHERE item_key = 'settings-facilities';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/practice',            updated_at = now() WHERE item_key = 'settings-practice';
UPDATE menu_item SET screen_slug = '/settings/layout-settings/config/fee-schedules',       updated_at = now() WHERE item_key = 'settings-fee-schedules';

-- Step 2: Add data page items directly under Settings (sibling to Layout Settings)
-- We insert one per menu that has a Settings parent
INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position, created_at, updated_at)
SELECT
    settings_parent.menu_id,
    settings_parent.id,
    'data-' || cfg.page_key,
    cfg.label,
    cfg.icon,
    '/settings/p/' || cfg.page_key,
    cfg.pos + 20,  -- offset after Layout Settings children
    now(), now()
FROM (VALUES
    ('providers',          'Providers',           'UserCog',     0),
    ('referral-providers', 'Referral Providers',  'UserPlus',    1),
    ('referral-practices', 'Referral Practices',  'Building',    2),
    ('insurance',          'Insurance Companies', 'Shield',      3),
    ('documents',          'Documents',           'FileText',    4),
    ('template-documents', 'Template Documents',  'FilePlus',    5),
    ('codes',              'Codes',               'FileCode',    6),
    ('integration',        'Integration',         'Plug',        7),
    ('services',           'Services',            'Briefcase',   8),
    ('billing',            'Billing',             'CreditCard',  9),
    ('forms',              'Forms',               'FileInput',   10),
    ('facilities',         'Facilities',          'Building2',   11),
    ('practice',           'Practice',            'Stethoscope', 12),
    ('fee-schedules',      'Fee Schedules',       'DollarSign',  13)
) AS cfg(page_key, label, icon, pos)
CROSS JOIN menu_item settings_parent
WHERE settings_parent.item_key = 'settings'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item existing
    WHERE existing.item_key = 'data-' || cfg.page_key
      AND existing.menu_id = settings_parent.menu_id
  );

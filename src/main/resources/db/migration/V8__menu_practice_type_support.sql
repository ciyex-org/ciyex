-- Add practice_type_code to menu table for specialty-specific menus
-- Resolution: org overrides → practice-type menu → default ('*') menu

-- Add column with default '*'
ALTER TABLE menu ADD COLUMN practice_type_code VARCHAR(100) NOT NULL DEFAULT '*';

-- Drop old unique constraint and create new one including practice_type_code
ALTER TABLE menu DROP CONSTRAINT menu_code_org_id_key;
ALTER TABLE menu ADD CONSTRAINT menu_code_org_practice_key UNIQUE (code, org_id, practice_type_code);

-- Drop and recreate the index
DROP INDEX IF EXISTS idx_menu_code_org;
CREATE INDEX idx_menu_code_org_pt ON menu(code, org_id, practice_type_code);

-- Clean up legacy cloned org menus (they're replaced by override system)
-- First delete their items, then the menus
DELETE FROM menu_item WHERE menu_id IN (SELECT id FROM menu WHERE org_id != '*');
DELETE FROM menu WHERE org_id != '*';

-- Seed specialty-specific menus for practice types that differ from the default
-- Dentistry: remove Labs, add Imaging
INSERT INTO menu (id, code, name, location, status, org_id, practice_type_code)
VALUES ('a0000000-0000-0000-0000-000000000010'::uuid, 'ehr-sidebar', 'EHR Sidebar - Dentistry', 'SIDEBAR', 'PUBLISHED', '*', 'general-dentistry');

INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e1000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'calendar',     'Calendar',      'Calendar',      '/calendar',     0),
  ('e1000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'appointments', 'Appointments',  'CalendarCheck', '/appointments', 1),
  ('e1000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'patients',     'Patients',      'Users',         NULL,            2),
  ('e1000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'inventory',    'Inventory',     'Package',       NULL,            3),
  ('e1000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'recall',       'Recall',        'Bell',          '/recall',       4),
  ('e1000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'imaging',      'Imaging',       'ScanLine',      NULL,            5),
  ('e1000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'reports',      'Reports',       'BarChart3',     NULL,            6),
  ('e1000000-0000-0000-0000-000000000008'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, NULL, 'settings',     'Settings',      'Settings',      NULL,            7);

-- Dentistry > Patients sub-items (includes Dental Chart)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e2000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'patient-list',     'Patient List',     'List',          '/patients',                  0),
  ('e2000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'dental-chart',     'Dental Chart',     'Grid3X3',       '/patients/dental-chart',     1),
  ('e2000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'perio-chart',      'Perio Chart',      'Activity',      '/patients/perio-chart',      2),
  ('e2000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'treatment-plans',  'Treatment Plans',  'ClipboardList', '/patients/treatment-plans',  3),
  ('e2000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'encounters',       'Encounters',       'ClipboardList', '/all-encounters',            4),
  ('e2000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'messaging',        'Messaging',        'MessageSquare', '/messaging',                 5),
  ('e2000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'codes-list',       'Codes List',       'FileCode',      '/patients/codes',            6),
  ('e2000000-0000-0000-0000-000000000008'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000003'::uuid, 'claim-management', 'Claim Management', 'Receipt',       '/patients/claim-management', 7);

-- Dentistry > Inventory sub-items (same as default)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e2000000-0000-0000-0000-000000000010'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000004'::uuid, 'inv-dashboard',   'Dashboard',   'LayoutDashboard', '/inventory-management',             0),
  ('e2000000-0000-0000-0000-000000000011'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000004'::uuid, 'inv-inventory',   'Inventory',   'Package',         '/inventory-management/inventory',   1),
  ('e2000000-0000-0000-0000-000000000012'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000004'::uuid, 'inv-orders',      'Orders',      'ShoppingCart',    '/inventory-management/orders',      2),
  ('e2000000-0000-0000-0000-000000000013'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000004'::uuid, 'inv-suppliers',   'Suppliers',   'Truck',           '/inventory-management/suppliers',   3);

-- Dentistry > Imaging sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e2000000-0000-0000-0000-000000000020'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000006'::uuid, 'imaging-xrays',       'X-Rays',       'Image',     '/imaging/xrays',       0),
  ('e2000000-0000-0000-0000-000000000021'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000006'::uuid, 'imaging-intraoral',   'Intraoral',    'Camera',    '/imaging/intraoral',   1),
  ('e2000000-0000-0000-0000-000000000022'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000006'::uuid, 'imaging-panoramic',   'Panoramic',    'Maximize2', '/imaging/panoramic',   2);

-- Dentistry > Reports sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e2000000-0000-0000-0000-000000000030'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000007'::uuid, 'report-patient',     'Patient Report',     'Users',         '/reports/patient',     0),
  ('e2000000-0000-0000-0000-000000000031'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000007'::uuid, 'report-appointment', 'Appointment Report', 'CalendarCheck', '/reports/appointment', 1),
  ('e2000000-0000-0000-0000-000000000032'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000007'::uuid, 'report-production',  'Production Report',  'DollarSign',    '/reports/production',  2);

-- Dentistry > Settings sub-items (same structure, add dental-specific)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e2000000-0000-0000-0000-000000000040'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-providers',          'Providers',             'UserCog',         '/settings/providers',             0),
  ('e2000000-0000-0000-0000-000000000041'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-referral-providers', 'Referral Providers',    'UserPlus',        '/settings/referral-providers',    1),
  ('e2000000-0000-0000-0000-000000000042'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-insurance',          'Insurance Companies',   'Shield',          '/settings/insurance',             2),
  ('e2000000-0000-0000-0000-000000000043'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-documents',          'Documents',             'FileText',        '/settings/Documents',             3),
  ('e2000000-0000-0000-0000-000000000044'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-codes',              'Codes',                 'FileCode',        '/settings/codes',                 4),
  ('e2000000-0000-0000-0000-000000000045'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-fee-schedules',      'Fee Schedules',         'DollarSign',      '/settings/fee-schedules',         5),
  ('e2000000-0000-0000-0000-000000000046'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-services',           'Services',              'Briefcase',       '/settings/services',              6),
  ('e2000000-0000-0000-0000-000000000047'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-billing',            'Billing',               'CreditCard',      '/settings/billing',               7),
  ('e2000000-0000-0000-0000-000000000048'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-practice',           'Practice',              'Stethoscope',     '/settings/practice',              8),
  ('e2000000-0000-0000-0000-000000000049'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-layout',             'Layout Settings',       'LayoutDashboard', NULL,                              9),
  ('e2000000-0000-0000-0000-000000000050'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e1000000-0000-0000-0000-000000000008'::uuid, 'settings-menu-config',        'Menu Configuration',    'Menu',            '/settings/menu-configuration',    10);

-- Dentistry > Settings > Layout Settings sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('e3000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000010'::uuid, 'e2000000-0000-0000-0000-000000000049'::uuid, 'settings-layout-config', 'Layout Configuration', 'LayoutGrid', '/settings/tab-configuration', 0);


-- Psychiatry/Psychology: Add assessments, treatment plans, progress notes
INSERT INTO menu (id, code, name, location, status, org_id, practice_type_code)
VALUES ('a0000000-0000-0000-0000-000000000011'::uuid, 'ehr-sidebar', 'EHR Sidebar - Psychiatry', 'SIDEBAR', 'PUBLISHED', '*', 'psychiatry');

INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f1000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'calendar',      'Calendar',       'Calendar',      '/calendar',     0),
  ('f1000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'appointments',  'Appointments',   'CalendarCheck', '/appointments', 1),
  ('f1000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'patients',      'Patients',       'Users',         NULL,            2),
  ('f1000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'assessments',   'Assessments',    'ClipboardCheck','NULL',          3),
  ('f1000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'recall',        'Recall',         'Bell',          '/recall',       4),
  ('f1000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'reports',       'Reports',        'BarChart3',     NULL,            5),
  ('f1000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, NULL, 'settings',      'Settings',       'Settings',      NULL,            6);

-- Psychiatry > Patients sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f2000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'patient-list',      'Patient List',      'List',          '/patients',                  0),
  ('f2000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'encounters',        'Sessions',          'ClipboardList', '/all-encounters',            1),
  ('f2000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'treatment-plans',   'Treatment Plans',   'FileCheck',     '/patients/treatment-plans',  2),
  ('f2000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'progress-notes',    'Progress Notes',    'FileText',      '/patients/progress-notes',   3),
  ('f2000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'messaging',         'Messaging',         'MessageSquare', '/messaging',                 4),
  ('f2000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000003'::uuid, 'claim-management',  'Claim Management',  'Receipt',       '/patients/claim-management', 5);

-- Psychiatry > Assessments sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f2000000-0000-0000-0000-000000000010'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000004'::uuid, 'phq9',    'PHQ-9',    'FileText', '/assessments/phq9',    0),
  ('f2000000-0000-0000-0000-000000000011'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000004'::uuid, 'gad7',    'GAD-7',    'FileText', '/assessments/gad7',    1),
  ('f2000000-0000-0000-0000-000000000012'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000004'::uuid, 'columbia', 'C-SSRS',  'FileText', '/assessments/columbia', 2),
  ('f2000000-0000-0000-0000-000000000013'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000004'::uuid, 'custom-assessments', 'All Assessments', 'ClipboardList', '/assessments', 3);

-- Psychiatry > Reports sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f2000000-0000-0000-0000-000000000020'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000006'::uuid, 'report-patient',     'Patient Report',     'Users',         '/reports/patient',     0),
  ('f2000000-0000-0000-0000-000000000021'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000006'::uuid, 'report-appointment', 'Appointment Report', 'CalendarCheck', '/reports/appointment', 1),
  ('f2000000-0000-0000-0000-000000000022'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000006'::uuid, 'report-encounter',   'Session Report',     'ClipboardList', '/reports/encounter',   2);

-- Psychiatry > Settings sub-items (compact)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f2000000-0000-0000-0000-000000000030'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-providers',   'Providers',          'UserCog',         '/settings/providers',          0),
  ('f2000000-0000-0000-0000-000000000031'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-insurance',   'Insurance Companies','Shield',          '/settings/insurance',          1),
  ('f2000000-0000-0000-0000-000000000032'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-documents',   'Documents',          'FileText',        '/settings/Documents',          2),
  ('f2000000-0000-0000-0000-000000000033'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-codes',       'Codes',              'FileCode',        '/settings/codes',              3),
  ('f2000000-0000-0000-0000-000000000034'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-services',    'Services',           'Briefcase',       '/settings/services',           4),
  ('f2000000-0000-0000-0000-000000000035'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-billing',     'Billing',            'CreditCard',      '/settings/billing',            5),
  ('f2000000-0000-0000-0000-000000000036'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-practice',    'Practice',           'Stethoscope',     '/settings/practice',           6),
  ('f2000000-0000-0000-0000-000000000037'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-layout',      'Layout Settings',    'LayoutDashboard', NULL,                           7),
  ('f2000000-0000-0000-0000-000000000038'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f1000000-0000-0000-0000-000000000007'::uuid, 'settings-menu-config', 'Menu Configuration', 'Menu',            '/settings/menu-configuration', 8);

-- Psychiatry > Settings > Layout Settings sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f3000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000011'::uuid, 'f2000000-0000-0000-0000-000000000037'::uuid, 'settings-layout-config', 'Layout Configuration', 'LayoutGrid', '/settings/tab-configuration', 0);


-- Physical Therapy: Add evaluations, exercise programs, goals
INSERT INTO menu (id, code, name, location, status, org_id, practice_type_code)
VALUES ('a0000000-0000-0000-0000-000000000012'::uuid, 'ehr-sidebar', 'EHR Sidebar - Physical Therapy', 'SIDEBAR', 'PUBLISHED', '*', 'physical-therapy');

INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f4000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'calendar',      'Calendar',       'Calendar',      '/calendar',     0),
  ('f4000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'appointments',  'Appointments',   'CalendarCheck', '/appointments', 1),
  ('f4000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'patients',      'Patients',       'Users',         NULL,            2),
  ('f4000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'inventory',     'Supplies',       'Package',       NULL,            3),
  ('f4000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'recall',        'Recall',         'Bell',          '/recall',       4),
  ('f4000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'reports',       'Reports',        'BarChart3',     NULL,            5),
  ('f4000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, NULL, 'settings',      'Settings',       'Settings',      NULL,            6);

-- PT > Patients sub-items (includes evaluations, exercise programs, goals)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f5000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'patient-list',      'Patient List',       'List',          '/patients',                     0),
  ('f5000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'evaluations',       'Evaluations',        'ClipboardCheck','/patients/evaluations',         1),
  ('f5000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'exercise-programs', 'Exercise Programs',  'Dumbbell',      '/patients/exercise-programs',   2),
  ('f5000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'goals',             'Goals & Outcomes',   'Target',        '/patients/goals',               3),
  ('f5000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'encounters',        'Visits',             'ClipboardList', '/all-encounters',               4),
  ('f5000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'messaging',         'Messaging',          'MessageSquare', '/messaging',                    5),
  ('f5000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000003'::uuid, 'claim-management',  'Claim Management',   'Receipt',       '/patients/claim-management',    6);

-- PT > Reports sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f5000000-0000-0000-0000-000000000010'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000006'::uuid, 'report-patient',      'Patient Report',      'Users',         '/reports/patient',      0),
  ('f5000000-0000-0000-0000-000000000011'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000006'::uuid, 'report-appointment',  'Appointment Report',  'CalendarCheck', '/reports/appointment',  1),
  ('f5000000-0000-0000-0000-000000000012'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000006'::uuid, 'report-productivity', 'Productivity Report', 'Timer',         '/reports/productivity', 2);

-- PT > Settings sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f5000000-0000-0000-0000-000000000020'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-providers',   'Providers',          'UserCog',         '/settings/providers',          0),
  ('f5000000-0000-0000-0000-000000000021'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-insurance',   'Insurance Companies','Shield',          '/settings/insurance',          1),
  ('f5000000-0000-0000-0000-000000000022'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-documents',   'Documents',          'FileText',        '/settings/Documents',          2),
  ('f5000000-0000-0000-0000-000000000023'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-codes',       'Codes',              'FileCode',        '/settings/codes',              3),
  ('f5000000-0000-0000-0000-000000000024'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-services',    'Services',           'Briefcase',       '/settings/services',           4),
  ('f5000000-0000-0000-0000-000000000025'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-billing',     'Billing',            'CreditCard',      '/settings/billing',            5),
  ('f5000000-0000-0000-0000-000000000026'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-practice',    'Practice',           'Stethoscope',     '/settings/practice',           6),
  ('f5000000-0000-0000-0000-000000000027'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-layout',      'Layout Settings',    'LayoutDashboard', NULL,                           7),
  ('f5000000-0000-0000-0000-000000000028'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f4000000-0000-0000-0000-000000000007'::uuid, 'settings-menu-config', 'Menu Configuration', 'Menu',            '/settings/menu-configuration', 8);

-- PT > Settings > Layout Settings sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
  ('f6000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000012'::uuid, 'f5000000-0000-0000-0000-000000000027'::uuid, 'settings-layout-config', 'Layout Configuration', 'LayoutGrid', '/settings/tab-configuration', 0);

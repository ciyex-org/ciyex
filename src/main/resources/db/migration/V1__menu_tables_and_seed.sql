-- Menu tables + EHR sidebar seed data

CREATE TABLE menu (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(50)  NOT NULL DEFAULT 'SIDEBAR',
    version     INT          NOT NULL DEFAULT 1,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    org_id      VARCHAR(100) NOT NULL DEFAULT '*',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (code, org_id)
);

CREATE TABLE menu_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id      UUID         NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    parent_id    UUID                  REFERENCES menu_item(id) ON DELETE CASCADE,
    item_key     VARCHAR(100) NOT NULL,
    label        VARCHAR(255) NOT NULL,
    icon         VARCHAR(100),
    screen_slug  VARCHAR(255),
    position     INT          NOT NULL DEFAULT 0,
    roles        JSONB,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_menu_code_org    ON menu(code, org_id);
CREATE INDEX idx_menu_item_menu   ON menu_item(menu_id);
CREATE INDEX idx_menu_item_parent ON menu_item(parent_id);

-- =====================================================
-- Row Level Security
-- =====================================================
ALTER TABLE menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu_item ENABLE ROW LEVEL SECURITY;

-- Menu: see global ('*') + own org rows
CREATE POLICY menu_tenant_policy ON menu
    USING (org_id = '*' OR org_id = current_setting('app.current_org', true));

-- Menu items: visible if their parent menu is visible
CREATE POLICY menu_item_tenant_policy ON menu_item
    USING (menu_id IN (
        SELECT id FROM menu
        WHERE org_id = '*' OR org_id = current_setting('app.current_org', true)
    ));

-- Superuser (postgres) bypasses RLS
ALTER TABLE menu FORCE ROW LEVEL SECURITY;
ALTER TABLE menu_item FORCE ROW LEVEL SECURITY;

-- App role for RLS (if not exists)
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user LOGIN PASSWORD 'app_user';
    END IF;
END $$;
GRANT SELECT, INSERT, UPDATE, DELETE ON menu, menu_item TO app_user;

-- =====================================================
-- Seed: ehr-sidebar (global default menu)
-- =====================================================
INSERT INTO menu (id, code, name, location, status, org_id)
VALUES ('a0000000-0000-0000-0000-000000000001', 'ehr-sidebar', 'EHR Sidebar Navigation', 'SIDEBAR', 'PUBLISHED', '*');

-- Top-level items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', NULL, 'calendar',      'Calendar',      'Calendar',      '/calendar',      0),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', NULL, 'appointments',   'Appointments',  'CalendarCheck', '/appointments',  1),
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', NULL, 'patients',       'Patients',      'Users',          NULL,            2),
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', NULL, 'inventory',      'Inventory',     'Package',        NULL,            3),
('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', NULL, 'recall',         'Recall',        'Bell',          '/recall',        4),
('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', NULL, 'reports',        'Reports',       'BarChart3',      NULL,            5),
('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', NULL, 'settings',       'Settings',      'Settings',       NULL,            6),
('b0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', NULL, 'labs',           'Labs',          'FlaskConical',   NULL,            7);

-- Patients sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'patient-list',     'Patient List',      'List',           '/patients',                0),
('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'encounters',       'Encounters',        'ClipboardList',  '/all-encounters',          1),
('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'messaging',        'Messaging',         'MessageSquare',  '/messaging',               2),
('c0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'education',        'Education',         'GraduationCap',  '/patient_education',       3),
('c0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'codes-list',       'Codes List',        'FileCode',       '/patients/codes',          4),
('c0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'claim-management', 'Claim Management',  'Receipt',        '/patients/claim-management', 5);

-- Inventory sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('c0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-dashboard',   'Dashboard',    'LayoutDashboard', '/inventory-management',             0),
('c0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-inventory',   'Inventory',    'Package',         '/inventory-management/inventory',   1),
('c0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-orders',      'Orders',       'ShoppingCart',    '/inventory-management/orders',      2),
('c0000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-records',     'Records',      'FileText',        '/inventory-management/records',     3),
('c0000000-0000-0000-0000-000000000014', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-suppliers',   'Suppliers',    'Truck',           '/inventory-management/suppliers',   4),
('c0000000-0000-0000-0000-000000000015', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-maintenance', 'Maintenance',  'Wrench',          '/inventory-management/maintenance', 5),
('c0000000-0000-0000-0000-000000000016', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'inv-settings',    'Settings',     'Settings',        '/inventory-management/settings',    6);

-- Reports sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('c0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', 'report-patient',      'Patient Report',      'Users',         '/reports/patient',      0),
('c0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', 'report-appointment',  'Appointment Report',  'CalendarCheck', '/reports/appointment',  1),
('c0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', 'report-encounter',    'Encounter Report',    'ClipboardList', '/reports/encounter',    2),
('c0000000-0000-0000-0000-000000000023', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', 'report-payment',      'Payment Reports',     'DollarSign',    '/reports/payment',      3);

-- Settings sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('c0000000-0000-0000-0000-000000000030', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-providers',          'Providers',           'UserCog',      '/settings/providers',          0),
('c0000000-0000-0000-0000-000000000031', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-referral-providers',  'Referral Providers',  'UserPlus',     '/settings/referral-providers',  1),
('c0000000-0000-0000-0000-000000000032', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-referral-practices',  'Referral Practices',  'Building',     '/settings/referral-practices',  2),
('c0000000-0000-0000-0000-000000000033', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-insurance',           'Insurance Companies', 'Shield',       '/settings/insurance',           3),
('c0000000-0000-0000-0000-000000000034', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-documents',           'Documents',           'FileText',     '/settings/Documents',           4),
('c0000000-0000-0000-0000-000000000035', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-template-documents',  'Template Documents',  'FilePlus',     '/settings/templateDocument',    5),
('c0000000-0000-0000-0000-000000000036', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-codes',               'Codes',               'FileCode',     '/settings/codes',               6),
('c0000000-0000-0000-0000-000000000037', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-integration',         'Integration',         'Plug',         '/settings/config',              7),
('c0000000-0000-0000-0000-000000000038', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-services',            'Services',            'Briefcase',    '/settings/services',            8),
('c0000000-0000-0000-0000-000000000039', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-billing',             'Billing',             'CreditCard',   '/settings/billing',             9),
('c0000000-0000-0000-0000-000000000040', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-forms',               'Forms',               'FileInput',     NULL,                          10),
('c0000000-0000-0000-0000-000000000041', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-facilities',          'Facilities',          'Building2',    '/settings/facilities',         11),
('c0000000-0000-0000-0000-000000000042', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-practice',            'Practice',            'Stethoscope',  '/settings/practice',           12),
('c0000000-0000-0000-0000-000000000043', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-tab-config',          'Tab Configuration',   'LayoutGrid',   '/settings/tab-configuration',  13),
('c0000000-0000-0000-0000-000000000044', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 'settings-menu-config',         'Menu Configuration',  'Menu',         '/settings/menu-configuration', 14);

-- Labs sub-items
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('c0000000-0000-0000-0000-000000000050', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000008', 'labs-orders',  'Lab Orders',  'TestTube',     '/labs/orders',  0),
('c0000000-0000-0000-0000-000000000051', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000008', 'labs-results', 'Lab Results',  'FileBarChart', '/labs/results', 1);

-- Settings > Forms sub-items (3rd level)
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES
('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000040', 'settings-forms-lists',      'Lists',              'List',          '/settings/forms/lists', 0),
('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000040', 'settings-forms-encounters', 'Encounter Sections', 'ClipboardList', '/settings/forms/admin', 1);

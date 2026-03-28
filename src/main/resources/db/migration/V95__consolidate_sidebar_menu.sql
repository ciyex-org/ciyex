-- V95: Consolidate sidebar menu — group flat items under collapsible parents
--
-- Current: 31 top-level items  →  Proposed: 14 top-level (6 flat + 6 parents + Hub + Dev)
--
-- Flat daily workflow (unchanged):
--   Calendar(0), Appointments(1), Patients(2), Encounters(3), Tasks(4), Messaging(5)
--
-- ▶ Clinical (10)       — Prescriptions, Labs, Immunizations, Referrals, Authorizations, Care Plans, Education
-- ▶ Operations (20)     — Recall, Codes, Inventory, Payments, Claim Management
-- ▶ Reports (25)        — Reports, AI Usage
-- ▶ System (30)         — Clinical Alerts, Consents, Notifications, Fax, Doc Scanning, Check-in Kiosk, Audit Log
-- ▶ Administration (37) — Users, Roles & Permissions  (already exists from V94)
-- ▶ Settings (80)       — General, Layout, Portal
--
-- Ciyex Hub (90), Developer Portal (91) — unchanged

DO $$
DECLARE
    v_menu      UUID := 'a0000000-0000-0000-0000-000000000001';
    v_clinical  UUID;
    v_ops       UUID;
    v_reports   UUID;
    v_system    UUID;
    v_settings  UUID;
    v_admin     UUID;
BEGIN

    -- ════════════════════════════════════════════
    -- 1. CREATE PARENT: Clinical (pos 10)
    -- ════════════════════════════════════════════
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, NULL, 'clinical', 'Clinical', 'Stethoscope', NULL, 10)
    RETURNING id INTO v_clinical;

    -- Move children under Clinical
    UPDATE menu_item SET parent_id = v_clinical, position = 0
    WHERE item_key = 'prescriptions' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 1
    WHERE item_key = 'labs' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 2
    WHERE item_key = 'immunizations' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 3
    WHERE item_key = 'referrals' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 4
    WHERE item_key = 'prior-auth' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 5
    WHERE item_key = 'care-plans' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_clinical, position = 6
    WHERE item_key = 'education' AND menu_id = v_menu AND parent_id IS NULL;

    -- ════════════════════════════════════════════
    -- 2. CREATE PARENT: Operations (pos 20)
    -- ════════════════════════════════════════════
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, NULL, 'operations', 'Operations', 'Briefcase', NULL, 20)
    RETURNING id INTO v_ops;

    UPDATE menu_item SET parent_id = v_ops, position = 0
    WHERE item_key = 'recall' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_ops, position = 1
    WHERE item_key = 'codes-list' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_ops, position = 2
    WHERE item_key = 'inventory' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_ops, position = 3
    WHERE item_key = 'payments' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_ops, position = 4
    WHERE item_key = 'claim-management' AND menu_id = v_menu AND parent_id IS NULL;

    -- ════════════════════════════════════════════
    -- 3. CREATE PARENT: Reports (pos 25)
    -- ════════════════════════════════════════════
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, NULL, 'reports-group', 'Reports', 'BarChart3', NULL, 25)
    RETURNING id INTO v_reports;

    -- Move existing flat "reports" item under the new parent
    UPDATE menu_item SET parent_id = v_reports, position = 0, item_key = 'reports-main'
    WHERE item_key = 'reports' AND menu_id = v_menu AND parent_id IS NULL;

    -- Move AI Usage from Administration → Reports
    -- Find administration parent
    SELECT id INTO v_admin FROM menu_item
    WHERE item_key = 'administration' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_reports, position = 1
    WHERE item_key = 'admin-ai-usage' AND menu_id = v_menu AND parent_id = v_admin;

    -- ════════════════════════════════════════════
    -- 4. CREATE PARENT: System (pos 30)
    -- ════════════════════════════════════════════
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, NULL, 'system', 'System', 'Settings', NULL, 30)
    RETURNING id INTO v_system;

    UPDATE menu_item SET parent_id = v_system, position = 0
    WHERE item_key = 'cds' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 1
    WHERE item_key = 'consents' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 2
    WHERE item_key = 'notifications' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 3
    WHERE item_key = 'fax' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 4
    WHERE item_key = 'document-scanning' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 5
    WHERE item_key = 'kiosk' AND menu_id = v_menu AND parent_id IS NULL;

    UPDATE menu_item SET parent_id = v_system, position = 6
    WHERE item_key = 'audit-log' AND menu_id = v_menu AND parent_id IS NULL;

    -- ════════════════════════════════════════════
    -- 5. CONVERT Settings from flat → parent with children
    -- ════════════════════════════════════════════
    SELECT id INTO v_settings FROM menu_item
    WHERE item_key = 'settings' AND menu_id = v_menu AND parent_id IS NULL;

    -- Remove screen_slug so it acts as collapsible parent
    UPDATE menu_item SET screen_slug = NULL WHERE id = v_settings;

    -- Child: General (the old /settings page)
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, v_settings, 'settings-general', 'General', 'Settings', '/settings', 0);

    -- Child: Layout
    INSERT INTO menu_item (menu_id, parent_id, item_key, label, icon, screen_slug, position)
    VALUES (v_menu, v_settings, 'settings-layout', 'Layout', 'LayoutDashboard', '/settings/layout-settings', 1);

    -- Move portal-settings under Settings parent
    UPDATE menu_item SET parent_id = v_settings, position = 2, item_key = 'settings-portal'
    WHERE item_key = 'portal-settings' AND menu_id = v_menu AND parent_id IS NULL;

END $$;

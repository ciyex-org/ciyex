-- V87: Reorder sidebar menu items to industry-standard EHR order
-- Also removes duplicates and cleans up Layout Settings (now part of Settings page)

-- ── 1. Remove duplicates (keep lower-id version) ──

-- Remove duplicate 'reports' (keep the one at position 9, remove position 10)
DELETE FROM menu_item WHERE id = 'b0000000-0000-0000-0000-000000000006';

-- Remove duplicate 'education' (keep 'BookOpen' one at /education, remove old /patient_education)
DELETE FROM menu_item WHERE id = 'c0000000-0000-0000-0000-000000000004';

-- Remove 'Layout Settings' from all menus (it's a Settings sub-page, not a top-level item)
DELETE FROM menu_item WHERE item_key = 'settings-layout';

-- ── 2. Reorder general sidebar (EHR Sidebar Navigation) ──
-- Industry-standard EHR ordering: clinical workflow → clinical tools → operations → admin

-- Clinical Workflow (daily use)
UPDATE menu_item SET position = 0  WHERE item_key = 'calendar'          AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 1  WHERE item_key = 'appointments'      AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 2  WHERE item_key = 'patient-list'      AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 3  WHERE item_key = 'encounters'        AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 4  WHERE item_key = 'tasks'             AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 5  WHERE item_key = 'messaging'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;

-- Clinical Tools (during patient care)
UPDATE menu_item SET position = 10 WHERE item_key = 'prescriptions'     AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 11 WHERE item_key = 'labs'              AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 12 WHERE item_key = 'immunizations'     AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 13 WHERE item_key = 'referrals'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 14 WHERE item_key = 'prior-auth'        AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 15 WHERE item_key = 'care-plans'        AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 16 WHERE item_key = 'education'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;

-- Operations
UPDATE menu_item SET position = 20 WHERE item_key = 'recall'            AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 21 WHERE item_key = 'codes-list'        AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 22 WHERE item_key = 'inventory'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 23 WHERE item_key = 'payments'          AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 24 WHERE item_key = 'claim-management'  AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 25 WHERE item_key = 'reports'           AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;

-- Admin / System
UPDATE menu_item SET position = 30 WHERE item_key = 'cds'               AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 31 WHERE item_key = 'consents'          AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 32 WHERE item_key = 'notifications'     AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 33 WHERE item_key = 'fax'               AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 34 WHERE item_key = 'document-scanning' AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 35 WHERE item_key = 'kiosk'             AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 36 WHERE item_key = 'audit-log'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;

-- Bottom section (settings, hub, etc.)
UPDATE menu_item SET position = 80 WHERE item_key = 'settings'          AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 81 WHERE item_key = 'portal-settings'   AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 90 WHERE item_key = 'ciyex-hub'         AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;
UPDATE menu_item SET position = 91 WHERE item_key = 'developer-portal'  AND menu_id = 'a0000000-0000-0000-0000-000000000001' AND parent_id IS NULL;

-- ── 3. Clean up specialty menus too ──
-- Remove Layout Settings from specialty menus
-- (Already deleted above via item_key = 'settings-layout')

-- Reorder Dentistry sidebar
UPDATE menu_item SET position = 0  WHERE item_key = 'calendar'    AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 1  WHERE item_key = 'appointments' AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 2  WHERE item_key = 'patients'    AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 5  WHERE item_key = 'imaging'     AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 10 WHERE item_key = 'recall'      AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 11 WHERE item_key = 'inventory'   AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 20 WHERE item_key = 'reports'     AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;
UPDATE menu_item SET position = 80 WHERE item_key = 'settings'    AND menu_id = 'a0000000-0000-0000-0000-000000000010' AND parent_id IS NULL;

-- Reorder Physical Therapy sidebar
UPDATE menu_item SET position = 0  WHERE item_key = 'calendar'    AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 1  WHERE item_key = 'appointments' AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 2  WHERE item_key = 'patients'    AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 10 WHERE item_key = 'recall'      AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 11 WHERE item_key = 'inventory'   AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 20 WHERE item_key = 'reports'     AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;
UPDATE menu_item SET position = 80 WHERE item_key = 'settings'    AND menu_id = 'a0000000-0000-0000-0000-000000000011' AND parent_id IS NULL;

-- Reorder Psychiatry sidebar
UPDATE menu_item SET position = 0  WHERE item_key = 'calendar'    AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 1  WHERE item_key = 'appointments' AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 2  WHERE item_key = 'patients'    AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 3  WHERE item_key = 'assessments' AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 10 WHERE item_key = 'recall'      AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 20 WHERE item_key = 'reports'     AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;
UPDATE menu_item SET position = 80 WHERE item_key = 'settings'    AND menu_id = 'a0000000-0000-0000-0000-000000000012' AND parent_id IS NULL;

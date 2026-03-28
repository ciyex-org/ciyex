-- Promote Patient sub-items to top-level navigation (same level as Calendar, Appointments, etc.)
-- Remove the "Patients" parent folder since its children are now top-level.

-- 1. Move the 6 sub-items to top level (parent_id = NULL) and assign new positions
UPDATE menu_item SET parent_id = NULL, position = 2  WHERE id = 'c0000000-0000-0000-0000-000000000001'; -- Patient List
UPDATE menu_item SET parent_id = NULL, position = 3  WHERE id = 'c0000000-0000-0000-0000-000000000002'; -- Encounters
UPDATE menu_item SET parent_id = NULL, position = 4  WHERE id = 'c0000000-0000-0000-0000-000000000003'; -- Messaging
UPDATE menu_item SET parent_id = NULL, position = 5  WHERE id = 'c0000000-0000-0000-0000-000000000004'; -- Education
UPDATE menu_item SET parent_id = NULL, position = 6  WHERE id = 'c0000000-0000-0000-0000-000000000005'; -- Codes List
UPDATE menu_item SET parent_id = NULL, position = 7  WHERE id = 'c0000000-0000-0000-0000-000000000006'; -- Claim Management

-- 2. Shift existing top-level items down to make room
UPDATE menu_item SET position = 8   WHERE id = 'b0000000-0000-0000-0000-000000000004'; -- Inventory
UPDATE menu_item SET position = 9   WHERE id = 'b0000000-0000-0000-0000-000000000005'; -- Recall
UPDATE menu_item SET position = 10  WHERE id = 'b0000000-0000-0000-0000-000000000006'; -- Reports
UPDATE menu_item SET position = 11  WHERE id = 'b0000000-0000-0000-0000-000000000007'; -- Settings
UPDATE menu_item SET position = 12  WHERE id = 'b0000000-0000-0000-0000-000000000008'; -- Labs

-- 3. Delete the now-empty "Patients" parent folder
DELETE FROM menu_item WHERE id = 'b0000000-0000-0000-0000-000000000003';

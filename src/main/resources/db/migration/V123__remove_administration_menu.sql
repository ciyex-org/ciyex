-- V123: Remove dedicated Administration menu group from sidebar
-- Users and Roles are now accessible from within Settings page
DELETE FROM menu_item
WHERE item_key IN ('admin-users', 'admin-roles', 'admin-ai-usage')
  AND menu_id = 'a0000000-0000-0000-0000-000000000001';

DELETE FROM menu_item
WHERE item_key = 'administration'
  AND menu_id = 'a0000000-0000-0000-0000-000000000001';

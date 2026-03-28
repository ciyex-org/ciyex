-- Add display_order column for dynamic role priority resolution
ALTER TABLE role_permission_config ADD COLUMN IF NOT EXISTS display_order INT DEFAULT 100;

-- Set priority for system roles (lower = higher priority)
UPDATE role_permission_config SET display_order = 10 WHERE role_name = 'ADMIN';
UPDATE role_permission_config SET display_order = 20 WHERE role_name = 'PROVIDER';
UPDATE role_permission_config SET display_order = 30 WHERE role_name = 'NURSE';
UPDATE role_permission_config SET display_order = 40 WHERE role_name = 'MA';
UPDATE role_permission_config SET display_order = 50 WHERE role_name = 'FRONT_DESK';
UPDATE role_permission_config SET display_order = 60 WHERE role_name = 'BILLING';
UPDATE role_permission_config SET display_order = 70 WHERE role_name = 'PATIENT';

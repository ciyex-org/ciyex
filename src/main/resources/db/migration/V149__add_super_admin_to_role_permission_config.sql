-- V149: Add SUPER_ADMIN to role_permission_config __SYSTEM__ templates
-- SUPER_ADMIN has the same page-level + SMART scopes as ADMIN.
-- Without this, SUPER_ADMIN users get empty permissions → frontend blocks all pages.

INSERT INTO role_permission_config (role_name, role_label, description, permissions, smart_scopes, is_system, is_active, org_alias, display_order)
SELECT 'SUPER_ADMIN', 'Super Administrator', 'Full system access (org-level super admin)',
       permissions, smart_scopes, true, true, '__SYSTEM__', 0
FROM role_permission_config
WHERE org_alias = '__SYSTEM__' AND role_name = 'ADMIN'
ON CONFLICT (org_alias, role_name) DO UPDATE
SET permissions = EXCLUDED.permissions,
    smart_scopes = EXCLUDED.smart_scopes,
    updated_at = now();

-- Also copy SUPER_ADMIN into any org that already has ADMIN but lacks SUPER_ADMIN
INSERT INTO role_permission_config (role_name, role_label, description, permissions, smart_scopes, is_system, is_active, org_alias, display_order)
SELECT 'SUPER_ADMIN', 'Super Administrator', 'Full system access (org-level super admin)',
       a.permissions, a.smart_scopes, true, true, a.org_alias, 0
FROM role_permission_config a
WHERE a.role_name = 'ADMIN'
  AND a.org_alias != '__SYSTEM__'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission_config s
      WHERE s.org_alias = a.org_alias AND s.role_name = 'SUPER_ADMIN'
  )
ON CONFLICT (org_alias, role_name) DO NOTHING;

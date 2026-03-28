-- V99: Fix role_permission_config __SYSTEM__ seed data
-- Correct permissions for NURSE, MA, FRONT_DESK, BILLING roles
-- and fix reports permission naming to enable read/write enforcement.
--
-- Issues fixed:
--   NURSE:      remove scheduling.read (nurses don't book appointments),
--               remove orders.create (read-only on orders),
--               add rx.read (nurses can view medications/immunizations)
--   MA:         remove scheduling.read (MAs don't book appointments),
--               add demographics.write, orders.read, rx.read,
--               documents.write, messaging.send
--   FRONT_DESK: add documents.read
--   BILLING:    remove demographics.read (billing shouldn't access patient demographics)
--   PROVIDER:   rename reports.clinical → reports.read (read-only; .clinical caused write bypass)
--   BILLING:    rename reports.financial → reports.read
--   ADMIN:      add reports.write to retain full write access after renaming

UPDATE role_permission_config
SET permissions = '["scheduling.read", "scheduling.write", "demographics.read", "demographics.write",
  "chart.read", "chart.write", "chart.sign",
  "orders.read", "orders.create", "orders.sign",
  "rx.read", "rx.prescribe",
  "billing.read", "billing.write", "billing.submit",
  "admin.users", "admin.settings", "admin.roles",
  "documents.read", "documents.write",
  "messaging.read", "messaging.send",
  "reports.read", "reports.write"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'ADMIN';

UPDATE role_permission_config
SET permissions = '["scheduling.read", "scheduling.write",
  "demographics.read", "demographics.write",
  "chart.read", "chart.write", "chart.sign",
  "orders.read", "orders.create", "orders.sign",
  "rx.read", "rx.prescribe",
  "documents.read", "documents.write",
  "messaging.read", "messaging.send",
  "reports.read"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'PROVIDER';

UPDATE role_permission_config
SET permissions = '["demographics.read", "demographics.write",
  "chart.read", "chart.write",
  "orders.read",
  "rx.read",
  "documents.read", "documents.write",
  "messaging.read", "messaging.send"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'NURSE';

UPDATE role_permission_config
SET permissions = '["demographics.read", "demographics.write",
  "chart.read", "chart.write",
  "orders.read",
  "rx.read",
  "documents.read", "documents.write",
  "messaging.read", "messaging.send"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'MA';

UPDATE role_permission_config
SET permissions = '["scheduling.read", "scheduling.write",
  "demographics.read", "demographics.write",
  "documents.read",
  "messaging.read", "messaging.send"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'FRONT_DESK';

UPDATE role_permission_config
SET permissions = '["billing.read", "billing.write", "billing.submit",
  "reports.read"]'::jsonb
WHERE org_alias = '__SYSTEM__' AND role_name = 'BILLING';

-- PATIENT: no changes needed (already correct)
-- demographics.read, chart.read, messaging.{read,send}, documents.read

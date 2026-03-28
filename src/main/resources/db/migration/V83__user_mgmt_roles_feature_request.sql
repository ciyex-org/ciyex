-- =============================================
-- V83: User Management, Roles & Permissions, Feature Request
-- =============================================

-- ── 1. Role & Permission Configuration ──
CREATE TABLE IF NOT EXISTS role_permission_config (
    id              BIGSERIAL PRIMARY KEY,
    role_name       VARCHAR(50)  NOT NULL,
    role_label      VARCHAR(100) NOT NULL,
    description     TEXT,
    permissions     JSONB        NOT NULL DEFAULT '[]',
    is_system       BOOLEAN      DEFAULT false,
    is_active       BOOLEAN      DEFAULT true,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP    DEFAULT now(),
    updated_at      TIMESTAMP    DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_role_perm_org_role
    ON role_permission_config(org_alias, role_name);
CREATE INDEX IF NOT EXISTS idx_role_perm_org
    ON role_permission_config(org_alias);

-- ── 2. Feature Request table (optional tracking) ──
CREATE TABLE IF NOT EXISTS feature_request (
    id              BIGSERIAL PRIMARY KEY,
    category        VARCHAR(50)  NOT NULL DEFAULT 'feature_request',
    subject         VARCHAR(500) NOT NULL,
    description     TEXT,
    user_email      VARCHAR(255),
    user_name       VARCHAR(255),
    status          VARCHAR(30)  DEFAULT 'submitted',
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP    DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feature_req_org ON feature_request(org_alias);

-- ── 3. Seed system roles (per-org, use __SYSTEM__ as template) ──
-- These are template roles; orgs get copies on first access
INSERT INTO role_permission_config (role_name, role_label, description, permissions, is_system, is_active, org_alias)
SELECT * FROM (VALUES
    ('ADMIN',      'Administrator',   'Full system access',
     '["scheduling.read","scheduling.write","demographics.read","demographics.write","chart.read","chart.write","chart.sign","orders.read","orders.create","orders.sign","rx.read","rx.prescribe","billing.read","billing.write","admin.users","admin.settings","admin.roles","documents.read","documents.write","messaging.read","messaging.send","reports.clinical","reports.financial"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('PROVIDER',   'Provider',        'Clinical access with prescribing and signing',
     '["scheduling.read","scheduling.write","demographics.read","demographics.write","chart.read","chart.write","chart.sign","orders.read","orders.create","orders.sign","rx.read","rx.prescribe","documents.read","documents.write","messaging.read","messaging.send","reports.clinical"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('NURSE',      'Nurse',           'Clinical access without prescribing',
     '["scheduling.read","demographics.read","demographics.write","chart.read","chart.write","orders.read","orders.create","documents.read","documents.write","messaging.read","messaging.send"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('MA',         'Medical Assistant','Vitals, intake, limited chart access',
     '["scheduling.read","demographics.read","chart.read","chart.write","documents.read","messaging.read"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('FRONT_DESK', 'Front Desk',      'Scheduling and demographics only',
     '["scheduling.read","scheduling.write","demographics.read","demographics.write","messaging.read","messaging.send"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('BILLING',    'Billing',         'Financial and coding access',
     '["billing.read","billing.write","demographics.read","reports.financial"]'::jsonb,
     true, true, '__SYSTEM__'),
    ('PATIENT',    'Patient',         'Portal access — own data only',
     '["demographics.read","chart.read","messaging.read","messaging.send","documents.read"]'::jsonb,
     true, true, '__SYSTEM__')
) AS v(role_name, role_label, description, permissions, is_system, is_active, org_alias)
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission_config WHERE org_alias = '__SYSTEM__' AND role_name = v.role_name
);

-- ── 4. Menu items for new settings pages ──
-- Note: These pages are accessed via the settings page sub-sidebar,
-- not as separate sidebar items. No menu_item entries needed.

-- Menu org overrides: store only diffs, not clones
-- This replaces the clone approach. Global menu is always the base.

CREATE TABLE menu_org_override (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      VARCHAR(100) NOT NULL,
    menu_code   VARCHAR(100) NOT NULL DEFAULT 'ehr-sidebar',
    item_id     UUID REFERENCES menu_item(id) ON DELETE CASCADE,  -- null for custom items
    action      VARCHAR(20) NOT NULL,  -- 'hide', 'modify', 'reorder', 'add'
    data        JSONB,                 -- {label, icon, screenSlug, position, parentId, itemKey}
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_moo_org ON menu_org_override(org_id, menu_code);
CREATE INDEX idx_moo_item ON menu_org_override(item_id);

-- RLS
ALTER TABLE menu_org_override ENABLE ROW LEVEL SECURITY;
CREATE POLICY moo_tenant_policy ON menu_org_override
    USING (org_id = current_setting('app.current_org', true));
ALTER TABLE menu_org_override FORCE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON menu_org_override TO app_user;

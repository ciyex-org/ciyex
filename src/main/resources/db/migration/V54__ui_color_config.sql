-- UI Color Configuration: stores color assignments for calendar entities
-- (visit types, providers, locations, etc.)

CREATE TABLE ui_color_config (
    id          BIGSERIAL PRIMARY KEY,
    org_id      VARCHAR(100) NOT NULL,
    category    VARCHAR(50) NOT NULL,      -- 'visit-type', 'provider', 'location'
    entity_key  VARCHAR(200) NOT NULL,     -- value/id of the entity
    entity_label VARCHAR(300),             -- display label (denormalized for fast lookup)
    bg_color    VARCHAR(20) NOT NULL DEFAULT '#6B7280',
    border_color VARCHAR(20) NOT NULL DEFAULT '#4B5563',
    text_color  VARCHAR(20) NOT NULL DEFAULT '#FFFFFF',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(org_id, category, entity_key)
);

CREATE INDEX idx_ui_color_config_org_cat ON ui_color_config(org_id, category);

-- Add "Calendar Colors" menu item under Layout Settings for each menu
INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position)
SELECT
    gen_random_uuid(),
    ls.menu_id,
    ls.id,
    'settings-calendar-colors',
    'Calendar Colors',
    'Palette',
    '/settings/calendar-colors',
    20
FROM menu_item ls
WHERE ls.item_key = 'settings-layout'
  AND NOT EXISTS (
    SELECT 1 FROM menu_item ex
    WHERE ex.item_key = 'settings-calendar-colors'
      AND ex.menu_id = ls.menu_id
  );

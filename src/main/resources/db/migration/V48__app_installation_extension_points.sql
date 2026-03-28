-- Add extension_points column to track which UI slots an app contributes to
-- e.g., ["patient-chart:tab", "patient-chart:action-bar", "encounter:toolbar"]
ALTER TABLE app_installations ADD COLUMN IF NOT EXISTS extension_points JSONB DEFAULT '[]';

CREATE INDEX IF NOT EXISTS idx_app_installations_extension_points
    ON app_installations USING gin(extension_points);

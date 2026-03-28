-- Remove the 'Settings' category from tab_field_config so these entries
-- don't appear in the Chart page Tab Manager. Settings pages are managed
-- via the sidebar menu, not the chart layout.
UPDATE tab_field_config
SET category = NULL, category_position = NULL
WHERE category = 'Settings';

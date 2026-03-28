-- V84: Remove old report sub-menu items
-- The reports page now has its own built-in sidebar with 23 reports across 6 categories.
-- Sub-items (Patient Report, Appointment Report, etc.) are no longer needed.

-- Delete all report sub-items across all practice types
DELETE FROM menu_item WHERE item_key IN (
    'report-patient',
    'report-appointment',
    'report-encounter',
    'report-payment',
    'report-production',
    'report-productivity'
);

-- Ensure parent "Reports" items point to /reports (not NULL)
UPDATE menu_item
SET screen_slug = '/reports'
WHERE item_key = 'reports' AND (screen_slug IS NULL OR screen_slug = '');

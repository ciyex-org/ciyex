-- V59: Fix GENERAL → General (V41 used uppercase by mistake) and ensure category positions

-- Fix the GENERAL typo from V41 (insurance-coverage tab)
UPDATE tab_field_config
SET category = 'General', category_position = 1
WHERE category = 'GENERAL';

-- Ensure all category positions are correct (refresh from V21 + new ones)
UPDATE tab_field_config SET category_position = 0 WHERE category = 'Overview';
UPDATE tab_field_config SET category_position = 1 WHERE category = 'General';
UPDATE tab_field_config SET category_position = 2 WHERE category = 'Encounters';
UPDATE tab_field_config SET category_position = 3 WHERE category = 'Clinical';
UPDATE tab_field_config SET category_position = 4 WHERE category = 'Claims';
UPDATE tab_field_config SET category_position = 5 WHERE category = 'Financial';
UPDATE tab_field_config SET category_position = 6 WHERE category = 'Apps';
UPDATE tab_field_config SET category_position = 7 WHERE category = 'Other';

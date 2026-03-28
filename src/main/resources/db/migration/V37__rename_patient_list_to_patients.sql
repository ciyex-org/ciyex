-- Rename "Patient List" to "Patients" now that it's a top-level nav item
UPDATE menu_item SET label = 'Patients' WHERE id = 'c0000000-0000-0000-0000-000000000001';

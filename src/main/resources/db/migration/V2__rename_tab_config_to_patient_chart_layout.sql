-- Rename "Tab Configuration" to "Patient Chart Layout"
UPDATE menu_item
SET label = 'Patient Chart Layout',
    item_key = 'settings-patient-chart-layout',
    updated_at = now()
WHERE id = 'c0000000-0000-0000-0000-000000000043';

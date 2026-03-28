-- V45: Fix patients menu icon from List to Users across all menus
UPDATE menu_item SET icon = 'Users' WHERE item_key = 'patient-list';

-- V98: Allow reference data tab_keys to be read by any authenticated user
-- providers, facilities, services, referral-practices, referral-providers are used as
-- reference data by non-admin roles (calendar dropdowns, appointment services, referrals).
-- The settings UI is already protected by menu_item.required_permission = 'admin'.
-- Setting required_permission = NULL allows any authenticated user to read them.

UPDATE tab_field_config SET required_permission = NULL
WHERE tab_key IN ('providers', 'facilities', 'services', 'referral-practices', 'referral-providers');

-- Keep 'practice' as admin-only since it's strictly a settings page

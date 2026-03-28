-- Fix telehealth extension_points (ensure settings:nav-item is included)
-- and add vendor configs for telehealth + RCM

-- Telehealth: add settings:nav-item + vendor config
UPDATE app_installations
SET extension_points = '["settings:nav-item","patient-chart:action-bar"]'::jsonb,
    config = config || '{"vendor_name":"Doxy.me","vendor_id":"doxy-001"}'::jsonb
WHERE app_slug = 'ciyex-telehealth';

-- RCM: add settings:nav-item + vendor config
UPDATE app_installations
SET extension_points = '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb,
    config = config || '{"vendor_name":"Waystar","vendor_id":"waystar-001","clearinghouse_name":"Availity","auto_scrub":true}'::jsonb
WHERE app_slug = 'ciyex-rcm';

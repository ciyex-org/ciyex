-- Seed app installations for all new SDK plugins (dev orgs)
-- These enable plugin UIs to render when the app is "installed"

-- Ciyex Eligibility (Insurance Verification)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000017', 'ciyex-eligibility', 'Insurance Verification', 'CLINICAL',
     'active',
     '{"vendor_name":"Availity","auto_verify_on_schedule":true,"auto_verify_days_before":2,"coverage_discovery":true,"patient_estimation":true}'::jsonb,
     '["settings:nav-item","patient-chart:banner-alert","patient-chart:tab"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000017', 'ciyex-eligibility', 'Insurance Verification', 'CLINICAL',
     'active',
     '{"vendor_name":"Availity","auto_verify_on_schedule":true,"auto_verify_days_before":2,"coverage_discovery":true,"patient_estimation":true}'::jsonb,
     '["settings:nav-item","patient-chart:banner-alert","patient-chart:tab"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex eRx (E-Prescribing)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000012', 'ciyex-erx', 'E-Prescribing', 'CLINICAL',
     'active',
     '{"vendor_name":"DrFirst","epcs_enabled":false,"check_interactions":true,"check_allergies":true,"check_duplicates":true,"formulary_check":true,"auto_generic":true}'::jsonb,
     '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000012', 'ciyex-erx', 'E-Prescribing', 'CLINICAL',
     'active',
     '{"vendor_name":"DrFirst","epcs_enabled":false,"check_interactions":true,"check_allergies":true,"check_duplicates":true,"formulary_check":true,"auto_generic":true}'::jsonb,
     '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex Lab (Lab Integration)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000013', 'ciyex-lab', 'Lab Integration', 'CLINICAL',
     'active',
     '{"vendor_name":"Health Gorilla","default_lab":"Quest Diagnostics","auto_match_results":true,"auto_notify_provider":true,"auto_notify_patient_normal":false}'::jsonb,
     '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000013', 'ciyex-lab', 'Lab Integration', 'CLINICAL',
     'active',
     '{"vendor_name":"Health Gorilla","default_lab":"Quest Diagnostics","auto_match_results":true,"auto_notify_provider":true,"auto_notify_patient_normal":false}'::jsonb,
     '["settings:nav-item","encounter:form-footer","patient-chart:tab"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex Fax
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000015', 'ciyex-fax', 'Cloud Fax', 'COMMUNICATION',
     'active',
     '{"vendor_name":"Documo","fax_number":"(555) 100-2000","include_cover_page":true,"auto_route_inbox":true}'::jsonb,
     '["settings:nav-item","encounter:toolbar"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000015', 'ciyex-fax', 'Cloud Fax', 'COMMUNICATION',
     'active',
     '{"vendor_name":"Documo","fax_number":"(555) 100-2000","include_cover_page":true,"auto_route_inbox":true}'::jsonb,
     '["settings:nav-item","encounter:toolbar"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex Notifications (SMS & Messaging)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000016', 'ciyex-notifications', 'Notifications & Messaging', 'COMMUNICATION',
     'active',
     '{"sms_vendor_name":"Twilio","appt_reminders_enabled":true,"appt_reminder_sms":true,"appt_reminder_email":true,"two_way_enabled":true,"tcpa_optin_required":true}'::jsonb,
     '["settings:nav-item"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000016', 'ciyex-notifications', 'Notifications & Messaging', 'COMMUNICATION',
     'active',
     '{"sms_vendor_name":"Twilio","appt_reminders_enabled":true,"appt_reminder_sms":true,"appt_reminder_email":true,"two_way_enabled":true,"tcpa_optin_required":true}'::jsonb,
     '["settings:nav-item"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex RPM (Remote Patient Monitoring)
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000014', 'ciyex-rpm', 'Remote Patient Monitoring', 'CLINICAL',
     'active',
     '{"vendor_name":"BioIntelliSense","devices_bp":true,"devices_glucose":true,"devices_weight":true,"devices_pulse_ox":true,"threshold_systolic_high":140,"threshold_glucose_high":250,"alert_on_threshold":true,"billing_auto_track":true}'::jsonb,
     '["settings:nav-item","patient-chart:tab"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000014', 'ciyex-rpm', 'Remote Patient Monitoring', 'CLINICAL',
     'active',
     '{"vendor_name":"BioIntelliSense","devices_bp":true,"devices_glucose":true,"devices_weight":true,"devices_pulse_ox":true,"threshold_systolic_high":140,"threshold_glucose_high":250,"alert_on_threshold":true,"billing_auto_track":true}'::jsonb,
     '["settings:nav-item","patient-chart:tab"]'::jsonb)
ON CONFLICT DO NOTHING;

-- Ciyex Telehealth (add settings extension point)
UPDATE app_installations
SET extension_points = '["settings:nav-item","patient-chart:action-bar"]'::jsonb
WHERE app_slug = 'ciyex-telehealth'
  AND extension_points IS NOT NULL
  AND NOT extension_points @> '"settings:nav-item"'::jsonb;

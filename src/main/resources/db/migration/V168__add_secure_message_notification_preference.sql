-- Enable secure_message_received email notification for all orgs using __SYSTEM__ default
INSERT INTO notification_preference (org_alias, event_type, email_enabled, sms_enabled)
VALUES ('__SYSTEM__', 'secure_message_received', true, false)
ON CONFLICT (org_alias, event_type) DO NOTHING;

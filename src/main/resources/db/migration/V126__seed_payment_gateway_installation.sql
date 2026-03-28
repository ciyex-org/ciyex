-- Seed Payment Gateway app installation for dev orgs
INSERT INTO app_installations (org_id, app_id, app_slug, app_name, app_category,
    status, config, extension_points)
VALUES
    ('sunrise-family-medicine', '10000000-0000-0000-0000-000000000011', 'payment-gateway', 'Payment Gateway', 'BILLING',
     'active',
     '{"active_processor":"stripe","stripe_mode":"TEST","accepted_methods":["CARD","CASH","CHECK"],"convenience_fee_enabled":false,"auto_receipt":true}'::jsonb,
     '["settings:nav-item"]'::jsonb),
    ('hinisoft', '10000000-0000-0000-0000-000000000011', 'payment-gateway', 'Payment Gateway', 'BILLING',
     'active',
     '{"active_processor":"stripe","stripe_mode":"TEST","accepted_methods":["CARD","CASH","CHECK"],"convenience_fee_enabled":false,"auto_receipt":true}'::jsonb,
     '["settings:nav-item"]'::jsonb)
ON CONFLICT DO NOTHING;

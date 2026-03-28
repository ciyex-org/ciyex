-- Restore category='Settings' for settings page entries.
-- V28 set category=NULL to hide them from Chart Tab Manager,
-- but Settings page needs category='Settings' to identify its entries.

UPDATE tab_field_config
SET category = 'Settings'
WHERE tab_key IN (
    'providers', 'referral-providers', 'referral-practices',
    'insurance', 'template-documents', 'codes', 'integration',
    'services', 'forms', 'facilities', 'practice', 'fee-schedules'
)
AND (category IS NULL OR category != 'Settings');

-- Add searchParams to Organization-based tabs so the generic FHIR handler
-- can filter by Organization.type (prov, ins, dept) instead of returning all orgs.

UPDATE tab_field_config
SET fhir_resources = '[{"type":"Organization","searchParams":{"type":"prov"}}]'
WHERE tab_key = 'practice';

UPDATE tab_field_config
SET fhir_resources = '[{"type":"Organization","searchParams":{"type":"ins"}},{"type":"Coverage"}]'
WHERE tab_key = 'insurance';

UPDATE tab_field_config
SET fhir_resources = '[{"type":"Organization","searchParams":{"type":"dept"}}]'
WHERE tab_key = 'referral-practices';

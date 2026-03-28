-- V114: Remove duplicate 'payments' tab from patient chart
-- The 'payments' tab (plural) is a stale duplicate of 'payment' (singular).
-- 'payment' uses Invoice with patientSearchParam=subject and works correctly.
-- 'payments' has no patientSearchParam (singleRecord mode) and always shows empty.

-- Delete the stale 'payments' tab config entry
DELETE FROM tab_field_config
WHERE tab_key = 'payments'
  AND practice_type_code = '*'
  AND org_id = '*';

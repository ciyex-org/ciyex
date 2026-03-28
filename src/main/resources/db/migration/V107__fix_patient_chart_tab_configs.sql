-- V107: Fix patient chart tab configurations
-- 1. Update fhirResources to object format with patientSearchParam for patient-scoped tabs
-- 2. Fix visit-notes, referrals, claim-denials, payments, statements, messaging tabs
-- 3. Create facility tab
-- 4. Fix messaging sender/recipient field types and showInTable

-- ─── 1. visit-notes: Add patientSearchParam so saved notes are linked to patient ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"DocumentReference","patientSearchParam":"subject"}]',
    updated_at = now()
WHERE tab_key = 'visit-notes'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND fhir_resources::text = '["DocumentReference"]';

-- ─── 2. referrals: Add patientSearchParam + showInTable on key fields ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"ServiceRequest","patientSearchParam":"subject"}]',
    field_config = '{
  "sections":[{
    "key":"referral-info","title":"Referral Information","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"referTo","label":"Refer To","type":"text","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"ServiceRequest","path":"performer[0].display","type":"string"}},
      {"key":"specialty","label":"Specialty","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"","label":"Select Specialty"},{"value":"cardiology","label":"Cardiology"},{"value":"dermatology","label":"Dermatology"},{"value":"endocrinology","label":"Endocrinology"},{"value":"gastroenterology","label":"Gastroenterology"},{"value":"neurology","label":"Neurology"},{"value":"oncology","label":"Oncology"},{"value":"orthopedics","label":"Orthopedics"},{"value":"psychiatry","label":"Psychiatry"},{"value":"pulmonology","label":"Pulmonology"},{"value":"rheumatology","label":"Rheumatology"},{"value":"urology","label":"Urology"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"ServiceRequest","path":"orderDetail[0].text","type":"string"}},
      {"key":"reason","label":"Reason","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"ServiceRequest","path":"reasonCode[0].text","type":"string"}},
      {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"},{"value":"asap","label":"ASAP"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"ServiceRequest","path":"priority","type":"code"}},
      {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"draft","label":"Draft"},{"value":"active","label":"Active"},{"value":"completed","label":"Completed"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"ServiceRequest","path":"status","type":"code"}},
      {"key":"date","label":"Date","type":"date","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"ServiceRequest","path":"authoredOn","type":"dateTime"}},
      {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"note[0].text","type":"string"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'referrals'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 3. claim-denials: Add patientSearchParam ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"ClaimResponse","patientSearchParam":"patient"}]',
    updated_at = now()
WHERE tab_key = 'claim-denials'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND fhir_resources::text = '["ClaimResponse"]';

-- ─── 4. statements (Invoice): Add patientSearchParam ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Invoice","patientSearchParam":"subject"}]',
    field_config = '{
  "sections":[{
    "key":"statement-info","title":"Statement Information","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"statementDate","label":"Statement Date","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"date","type":"dateTime"}},
      {"key":"invoiceNumber","label":"Invoice Number","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"identifier[0].value","type":"string"}},
      {"key":"balance","label":"Balance Due","type":"number","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"totalGross.value","type":"decimal"}},
      {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"draft","label":"Draft"},{"value":"issued","label":"Issued"},{"value":"balanced","label":"Balanced"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"Invoice","path":"status","type":"code"}},
      {"key":"totalNet","label":"Total Net","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"totalNet.value","type":"decimal"}},
      {"key":"recipientName","label":"Recipient","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"recipient.display","type":"string"}},
      {"key":"issuerName","label":"Issuer","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"issuer.display","type":"string"}},
      {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"Invoice","path":"note[0].text","type":"string"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'statements'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 5. payments (PaymentReconciliation): Use object format, add showInTable ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"PaymentReconciliation","patientSearchParam":""}]',
    field_config = '{
  "sections":[{
    "key":"payment-info","title":"Payment Information","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"paymentDate","label":"Payment Date","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"PaymentReconciliation","path":"created","type":"dateTime"}},
      {"key":"amount","label":"Amount","type":"number","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentAmount.value","type":"decimal"}},
      {"key":"method","label":"Payment Method","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"cash","label":"Cash"},{"value":"check","label":"Check"},{"value":"credit-card","label":"Credit Card"},{"value":"eft","label":"EFT"},{"value":"insurance","label":"Insurance"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIdentifier.type.coding[0].code","type":"code"}},
      {"key":"reference","label":"Reference Number","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIdentifier.value","type":"string"}},
      {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"status","type":"code"}},
      {"key":"outcome","label":"Outcome","type":"select","required":false,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"outcome","type":"code"}},
      {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"PaymentReconciliation","path":"processNote[0].text","type":"string"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'payments'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND fhir_resources::text = '["PaymentReconciliation"]';

-- ─── 6. messaging: Fix fhirResources + fix sender/recipient to use text type with showInTable ───
UPDATE tab_field_config
SET fhir_resources = '[{"type":"Communication","patientSearchParam":"subject"}]',
    field_config = '{
  "sections":[{
    "key":"message-info","title":"Message","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"subject","label":"Subject","type":"text","required":true,"colSpan":2,"showInTable":true,"fhirMapping":{"resource":"Communication","path":"topic.text","type":"string"}},
      {"key":"message","label":"Message","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"Communication","path":"payload[0].contentString","type":"string"}},
      {"key":"sender","label":"From","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Communication","path":"sender.display","type":"string"}},
      {"key":"recipient","label":"To","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Communication","path":"recipient[0].display","type":"string"}},
      {"key":"sent","label":"Sent Date","type":"datetime","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Communication","path":"sent","type":"dateTime"}},
      {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"}],"fhirMapping":{"resource":"Communication","path":"priority","type":"code"}},
      {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"preparation","label":"Draft"},{"value":"in-progress","label":"In Progress"},{"value":"completed","label":"Completed"}],"fhirMapping":{"resource":"Communication","path":"status","type":"code"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'messaging'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 7. visit-notes: Add showInTable to key fields for list view ───
UPDATE tab_field_config
SET field_config = '{
  "sections":[{
    "key":"note-info","title":"Visit Note","columns":2,"collapsible":true,"collapsed":false,
    "fields":[
      {"key":"date","label":"Date","type":"date","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"DocumentReference","path":"date","type":"instant"}},
      {"key":"type","label":"Note Type","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"progress","label":"Progress Note"},{"value":"soap","label":"SOAP Note"},{"value":"procedure","label":"Procedure Note"},{"value":"discharge","label":"Discharge Summary"},{"value":"consultation","label":"Consultation Note"}],"fhirMapping":{"resource":"DocumentReference","path":"type.coding[0].code","type":"code"}},
      {"key":"author","label":"Author","type":"text","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"DocumentReference","path":"author[0].display","type":"string"}},
      {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"current","label":"Current"},{"value":"superseded","label":"Superseded"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"status","type":"code"}},
      {"key":"content","label":"Note Content","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"DocumentReference","path":"content[0].attachment.data","type":"string"}}
    ]
  }]
}',
    updated_at = now()
WHERE tab_key = 'visit-notes'
  AND practice_type_code = '*'
  AND org_id = '*';

-- ─── 8. Create facility tab (Location-based) if not exists ───
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, label, icon, category, category_position, position, visible)
SELECT
  'facility', '*', '*',
  '[{"type":"Location","patientSearchParam":""}]',
  '{
    "sections":[{
      "key":"facility-info","title":"Facility Information","columns":2,"collapsible":true,"collapsed":false,
      "fields":[
        {"key":"name","label":"Facility Name","type":"text","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Location","path":"name","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"showInTable":true,"options":[{"value":"active","label":"Active"},{"value":"suspended","label":"Suspended"},{"value":"inactive","label":"Inactive"}],"fhirMapping":{"resource":"Location","path":"status","type":"code"}},
        {"key":"address","label":"Address","type":"text","required":false,"colSpan":2,"fhirMapping":{"resource":"Location","path":"address.text","type":"string"}},
        {"key":"phone","label":"Phone","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Location","path":"telecom[0].value","type":"string"}},
        {"key":"type","label":"Type","type":"select","required":false,"colSpan":1,"options":[{"value":"HOSP","label":"Hospital"},{"value":"OUTPHARM","label":"Outpatient Pharmacy"},{"value":"CLINIC","label":"Clinic"},{"value":"LAB","label":"Laboratory"},{"value":"IMAG","label":"Imaging"}],"fhirMapping":{"resource":"Location","path":"type[0].coding[0].code","type":"code"}},
        {"key":"description","label":"Description","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"Location","path":"description","type":"string"}}
      ]
    }]
  }',
  'Facility', 'Building2', 'General', 10, 10, true
WHERE NOT EXISTS (
  SELECT 1 FROM tab_field_config WHERE tab_key = 'facility' AND practice_type_code = '*' AND org_id = '*'
);

-- ─── 9. Immunization: Add vaccine options to vaccineCode field ───
-- Update immunization tab to use a select for vaccine instead of "coded" type
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"key":"cvxCode","label":"Vaccine (CVX)","type":"coded"',
    '"key":"vaccineCode","label":"Vaccine","type":"select","showInTable":true'
)::jsonb,
    updated_at = now()
WHERE tab_key = 'immunizations'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND field_config::text LIKE '%"type":"coded"%';

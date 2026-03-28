-- Add FHIR resource mappings and skeleton field configs for tabs missing them
-- Also fix problem-list vs problems mismatch

UPDATE tab_field_config SET tab_key = 'problem-list' WHERE tab_key = 'problems' AND practice_type_code = '*' AND org_id = '*';

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES

('appointments', '*', '*', '["Appointment"]',
'{"sections":[{"key":"appointment-info","title":"Appointment Information","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"date","label":"Date & Time","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"start","type":"instant"}},
  {"key":"endDate","label":"End Time","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"end","type":"instant"}},
  {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"proposed","label":"Proposed"},{"value":"pending","label":"Pending"},{"value":"booked","label":"Booked"},{"value":"arrived","label":"Arrived"},{"value":"fulfilled","label":"Fulfilled"},{"value":"cancelled","label":"Cancelled"},{"value":"noshow","label":"No Show"}],"fhirMapping":{"resource":"Appointment","path":"status","type":"code"}},
  {"key":"type","label":"Visit Type","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"followup","label":"Follow-up"},{"value":"urgent","label":"Urgent"},{"value":"new-patient","label":"New Patient"},{"value":"telehealth","label":"Telehealth"}],"fhirMapping":{"resource":"Appointment","path":"appointmentType.coding[0].code","type":"code"}},
  {"key":"provider","label":"Provider","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"participant[0].actor","type":"Reference"}},
  {"key":"location","label":"Location","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"participant[1].actor","type":"Reference"}},
  {"key":"reason","label":"Reason","type":"text","required":false,"colSpan":2,"fhirMapping":{"resource":"Appointment","path":"reasonCode[0].text","type":"string"}},
  {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"Appointment","path":"comment","type":"string"}}
]}]}'),

('referrals', '*', '*', '["ServiceRequest"]',
'{"sections":[{"key":"referral-info","title":"Referral Information","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"referTo","label":"Refer To","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"performer[0].display","type":"string"}},
  {"key":"specialty","label":"Specialty","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"orderDetail[0].text","type":"string"}},
  {"key":"reason","label":"Reason","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"ServiceRequest","path":"reasonCode[0].text","type":"string"}},
  {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"},{"value":"asap","label":"ASAP"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"ServiceRequest","path":"priority","type":"code"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"active","label":"Active"},{"value":"completed","label":"Completed"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"ServiceRequest","path":"status","type":"code"}},
  {"key":"date","label":"Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"authoredOn","type":"dateTime"}},
  {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"note[0].text","type":"string"}}
]}]}'),

('visit-notes', '*', '*', '["DocumentReference"]',
'{"sections":[{"key":"note-info","title":"Visit Note","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"date","label":"Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"date","type":"instant"}},
  {"key":"type","label":"Note Type","type":"select","required":false,"colSpan":1,"options":[{"value":"progress","label":"Progress Note"},{"value":"soap","label":"SOAP Note"},{"value":"procedure","label":"Procedure Note"},{"value":"discharge","label":"Discharge Summary"},{"value":"consultation","label":"Consultation Note"}],"fhirMapping":{"resource":"DocumentReference","path":"type.coding[0].code","type":"code"}},
  {"key":"author","label":"Author","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"author[0]","type":"Reference"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"current","label":"Current"},{"value":"superseded","label":"Superseded"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"status","type":"code"}},
  {"key":"content","label":"Note Content","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"DocumentReference","path":"content[0].attachment.data","type":"string"}}
]}]}'),

('claims', '*', '*', '["Claim"]',
'{"sections":[{"key":"claim-info","title":"Claim Information","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"claimType","label":"Claim Type","type":"select","required":true,"colSpan":1,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"},{"value":"pharmacy","label":"Pharmacy"},{"value":"vision","label":"Vision"}],"fhirMapping":{"resource":"Claim","path":"type.coding[0].code","type":"code"}},
  {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}},
  {"key":"provider","label":"Billing Provider","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"provider","type":"Reference"}},
  {"key":"insurer","label":"Insurer","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"insurer","type":"Reference"}},
  {"key":"totalAmount","label":"Total Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"total.value","type":"decimal"}},
  {"key":"serviceDate","label":"Service Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"billablePeriod.start","type":"date"}},
  {"key":"facility","label":"Facility","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"facility","type":"Reference"}},
  {"key":"use","label":"Use","type":"select","required":false,"colSpan":1,"options":[{"value":"claim","label":"Claim"},{"value":"preauthorization","label":"Pre-authorization"},{"value":"predetermination","label":"Pre-determination"}],"fhirMapping":{"resource":"Claim","path":"use","type":"code"}}
]}]}'),

('claim-submissions', '*', '*', '["Claim"]',
'{"sections":[{"key":"submission-info","title":"Submission Details","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"submissionDate","label":"Submission Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"created","type":"dateTime"}},
  {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"pending","label":"Pending"},{"value":"accepted","label":"Accepted"},{"value":"rejected","label":"Rejected"},{"value":"paid","label":"Paid"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}},
  {"key":"clearinghouse","label":"Clearinghouse","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"extension[url=clearinghouse].valueString","type":"string"}},
  {"key":"trackingNumber","label":"Tracking Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"identifier[0].value","type":"string"}},
  {"key":"totalCharge","label":"Total Charge","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"total.value","type":"decimal"}},
  {"key":"insurer","label":"Insurer","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"insurer","type":"Reference"}}
]}]}'),

('claim-denials', '*', '*', '["ClaimResponse"]',
'{"sections":[{"key":"denial-info","title":"Denial Details","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"denialDate","label":"Denial Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"created","type":"dateTime"}},
  {"key":"disposition","label":"Disposition","type":"text","required":false,"colSpan":2,"fhirMapping":{"resource":"ClaimResponse","path":"disposition","type":"string"}},
  {"key":"outcome","label":"Outcome","type":"select","required":true,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"ClaimResponse","path":"outcome","type":"code"}},
  {"key":"status","label":"Appeal Status","type":"select","required":false,"colSpan":1,"options":[{"value":"pending","label":"Pending Appeal"},{"value":"appealed","label":"Appealed"},{"value":"upheld","label":"Upheld"},{"value":"overturned","label":"Overturned"}],"fhirMapping":{"resource":"ClaimResponse","path":"status","type":"code"}},
  {"key":"amountDenied","label":"Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"total[0].amount.value","type":"decimal"}},
  {"key":"insurer","label":"Insurer","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"insurer","type":"Reference"}}
]}]}'),

('era-remittance', '*', '*', '["ExplanationOfBenefit"]',
'{"sections":[{"key":"era-info","title":"ERA / Remittance","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"checkDate","label":"Check/EFT Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.date","type":"date"}},
  {"key":"checkNumber","label":"Check/EFT Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.identifier.value","type":"string"}},
  {"key":"payerName","label":"Payer","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"insurer","type":"Reference"}},
  {"key":"totalPaid","label":"Total Paid","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.amount.value","type":"decimal"}},
  {"key":"outcome","label":"Outcome","type":"select","required":false,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"ExplanationOfBenefit","path":"outcome","type":"code"}},
  {"key":"disposition","label":"Disposition","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"disposition","type":"string"}}
]}]}'),

('payments', '*', '*', '["PaymentReconciliation"]',
'{"sections":[{"key":"payment-info","title":"Payment Information","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"paymentDate","label":"Payment Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"created","type":"dateTime"}},
  {"key":"amount","label":"Amount","type":"number","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentAmount.value","type":"decimal"}},
  {"key":"method","label":"Payment Method","type":"select","required":false,"colSpan":1,"options":[{"value":"cash","label":"Cash"},{"value":"check","label":"Check"},{"value":"credit-card","label":"Credit Card"},{"value":"eft","label":"EFT"},{"value":"insurance","label":"Insurance"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIdentifier.type.coding[0].code","type":"code"}},
  {"key":"reference","label":"Reference Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIdentifier.value","type":"string"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"status","type":"code"}},
  {"key":"outcome","label":"Outcome","type":"select","required":false,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"outcome","type":"code"}}
]}]}'),

('statements', '*', '*', '["Invoice"]',
'{"sections":[{"key":"statement-info","title":"Statement Information","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"statementDate","label":"Statement Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"date","type":"dateTime"}},
  {"key":"balance","label":"Balance Due","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"totalGross.value","type":"decimal"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"issued","label":"Issued"},{"value":"balanced","label":"Balanced"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"Invoice","path":"status","type":"code"}},
  {"key":"totalNet","label":"Total Net","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"totalNet.value","type":"decimal"}},
  {"key":"recipient","label":"Recipient","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"recipient","type":"Reference"}},
  {"key":"issuer","label":"Issuer","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"issuer","type":"Reference"}}
]}]}'),

('education', '*', '*', '["Communication"]',
'{"sections":[{"key":"education-info","title":"Patient Education","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"topic","label":"Topic","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"topic.text","type":"string"}},
  {"key":"dateProvided","label":"Date Provided","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"sent","type":"dateTime"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"preparation","label":"Preparation"},{"value":"in-progress","label":"In Progress"},{"value":"completed","label":"Completed"}],"fhirMapping":{"resource":"Communication","path":"status","type":"code"}},
  {"key":"category","label":"Category","type":"select","required":false,"colSpan":1,"options":[{"value":"handout","label":"Handout"},{"value":"video","label":"Video"},{"value":"verbal","label":"Verbal"},{"value":"online","label":"Online Resource"}],"fhirMapping":{"resource":"Communication","path":"category[0].coding[0].code","type":"code"}},
  {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"Communication","path":"payload[0].contentString","type":"string"}}
]}]}'),

('messaging', '*', '*', '["Communication"]',
'{"sections":[{"key":"message-info","title":"Message","columns":2,"collapsible":true,"collapsed":false,"fields":[
  {"key":"subject","label":"Subject","type":"text","required":true,"colSpan":2,"fhirMapping":{"resource":"Communication","path":"topic.text","type":"string"}},
  {"key":"message","label":"Message","type":"textarea","required":true,"colSpan":2,"fhirMapping":{"resource":"Communication","path":"payload[0].contentString","type":"string"}},
  {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"}],"fhirMapping":{"resource":"Communication","path":"priority","type":"code"}},
  {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"preparation","label":"Draft"},{"value":"in-progress","label":"In Progress"},{"value":"completed","label":"Completed"}],"fhirMapping":{"resource":"Communication","path":"status","type":"code"}},
  {"key":"sender","label":"From","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"sender","type":"Reference"}},
  {"key":"recipient","label":"To","type":"reference","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"recipient[0]","type":"Reference"}}
]}]}')

ON CONFLICT (tab_key, practice_type_code, org_id) DO NOTHING;

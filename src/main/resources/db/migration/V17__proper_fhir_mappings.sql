-- V17: Update FHIR resource mappings with proper R4 field paths and patientSearchParam
-- Changes fhir_resources from simple array ["Appointment"] to objects with search metadata
-- Updates field_config with researched FHIR R4 paths from HL7 specs + codebase DTOs

-- =====================================================
-- 1. APPOINTMENTS → Appointment
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Appointment","patientSearchParam":"patient"}]',
  field_config = '{
  "sections": [
    {
      "key": "appointment-details",
      "title": "Appointment Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"proposed","label":"Proposed"},{"value":"pending","label":"Pending"},{"value":"booked","label":"Booked"},{"value":"arrived","label":"Arrived"},{"value":"fulfilled","label":"Fulfilled"},{"value":"cancelled","label":"Cancelled"},{"value":"noshow","label":"No Show"},{"value":"checked-in","label":"Checked In"}],"fhirMapping":{"resource":"Appointment","path":"status","type":"code"}},
        {"key":"appointmentType","label":"Visit Type","type":"select","required":false,"colSpan":1,"options":[{"value":"ROUTINE","label":"Routine"},{"value":"FOLLOWUP","label":"Follow-up"},{"value":"WALKIN","label":"Walk-in"},{"value":"CHECKUP","label":"Check-up"},{"value":"EMERGENCY","label":"Emergency"},{"value":"NEW_PATIENT","label":"New Patient"}],"fhirMapping":{"resource":"Appointment","path":"appointmentType.coding[0].code","type":"code"}},
        {"key":"serviceType","label":"Service Type","type":"text","required":false,"colSpan":1,"placeholder":"Service type","fhirMapping":{"resource":"Appointment","path":"serviceType[0].text","type":"string"}},
        {"key":"start","label":"Start Date/Time","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"start","type":"instant"}},
        {"key":"end","label":"End Date/Time","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"end","type":"instant"}},
        {"key":"minutesDuration","label":"Duration (min)","type":"number","required":false,"colSpan":1,"placeholder":"minutes","fhirMapping":{"resource":"Appointment","path":"minutesDuration","type":"positiveInt"}},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"1","label":"ASAP"},{"value":"2","label":"Callback results"},{"value":"3","label":"No rush"},{"value":"5","label":"Routine"},{"value":"9","label":"Low priority"}],"fhirMapping":{"resource":"Appointment","path":"priority","type":"unsignedInt"}},
        {"key":"reason","label":"Reason for Visit","type":"text","required":false,"colSpan":2,"placeholder":"Chief complaint / reason","fhirMapping":{"resource":"Appointment","path":"reasonCode[0].text","type":"string"}},
        {"key":"description","label":"Description","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"description","type":"string"}},
        {"key":"patientInstruction","label":"Patient Instructions","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Appointment","path":"patientInstruction","type":"string"}},
        {"key":"comment","label":"Internal Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Appointment","path":"comment","type":"string"}},
        {"key":"cancelationReason","label":"Cancellation Reason","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Appointment","path":"cancelationReason.text","type":"string"}}
      ]
    },
    {
      "key": "participants",
      "title": "Participants",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"patient","label":"Patient","type":"lookup","required":true,"colSpan":1,"lookupConfig":{"endpoint":"/api/patients","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Appointment","path":"participant[0].actor.reference","type":"reference"}},
        {"key":"provider","label":"Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Appointment","path":"participant[1].actor.reference","type":"reference"}},
        {"key":"location","label":"Location","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/locations","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Appointment","path":"participant[2].actor.reference","type":"reference"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 2. REFERRALS → ServiceRequest
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"ServiceRequest","patientSearchParam":"subject"}]',
  field_config = '{
  "sections": [
    {
      "key": "referral-details",
      "title": "Referral Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"active","label":"Active"},{"value":"on-hold","label":"On Hold"},{"value":"revoked","label":"Revoked"},{"value":"completed","label":"Completed"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"ServiceRequest","path":"status","type":"code"}},
        {"key":"intent","label":"Intent","type":"select","required":true,"colSpan":1,"options":[{"value":"order","label":"Order"},{"value":"original-order","label":"Original Order"},{"value":"plan","label":"Plan"},{"value":"proposal","label":"Proposal"}],"fhirMapping":{"resource":"ServiceRequest","path":"intent","type":"code"}},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"},{"value":"asap","label":"ASAP"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"ServiceRequest","path":"priority","type":"code"}},
        {"key":"code","label":"Service/Referral Type","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"code.text","type":"string"}},
        {"key":"category","label":"Category","type":"select","required":false,"colSpan":1,"options":[{"value":"108252007","label":"Referral"},{"value":"306206005","label":"Consultation"},{"value":"11429006","label":"Diagnostic"},{"value":"363679005","label":"Surgical"}],"fhirMapping":{"resource":"ServiceRequest","path":"category[0].coding[0].code","type":"code","system":"http://snomed.info/sct"}},
        {"key":"performerType","label":"Specialty","type":"text","required":false,"colSpan":1,"placeholder":"e.g. Cardiology","fhirMapping":{"resource":"ServiceRequest","path":"performerType.text","type":"string"}},
        {"key":"reasonCode","label":"Reason","type":"textarea","required":true,"colSpan":3,"fhirMapping":{"resource":"ServiceRequest","path":"reasonCode[0].text","type":"string"}},
        {"key":"authoredOn","label":"Referral Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"authoredOn","type":"dateTime"}},
        {"key":"occurrenceDateTime","label":"Requested Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"occurrenceDateTime","type":"dateTime"}},
        {"key":"note","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"note[0].text","type":"string"}},
        {"key":"patientInstruction","label":"Patient Instructions","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"ServiceRequest","path":"patientInstruction","type":"string"}}
      ]
    },
    {
      "key": "referral-parties",
      "title": "Referring & Referred-To",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"requester","label":"Referring Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"ServiceRequest","path":"requester.reference","type":"reference"}},
        {"key":"performer","label":"Referred-To Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"ServiceRequest","path":"performer[0].reference","type":"reference"}},
        {"key":"locationCode","label":"Preferred Location","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ServiceRequest","path":"locationCode[0].text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'referrals' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 3. VISIT NOTES → DocumentReference + Composition
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"DocumentReference","patientSearchParam":"subject"},{"type":"Composition","patientSearchParam":"subject"}]',
  field_config = '{
  "sections": [
    {
      "key": "note-header",
      "title": "Note Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"current","label":"Current"},{"value":"superseded","label":"Superseded"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"status","type":"code"}},
        {"key":"docStatus","label":"Document Status","type":"select","required":false,"colSpan":1,"options":[{"value":"preliminary","label":"Preliminary"},{"value":"final","label":"Final"},{"value":"amended","label":"Amended"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"docStatus","type":"code"}},
        {"key":"noteType","label":"Note Type","type":"select","required":true,"colSpan":1,"options":[{"value":"11506-3","label":"Progress Note"},{"value":"34109-9","label":"SOAP Note"},{"value":"28570-0","label":"Procedure Note"},{"value":"18842-5","label":"Discharge Summary"},{"value":"11488-4","label":"Consultation Note"},{"value":"34117-2","label":"History & Physical"}],"fhirMapping":{"resource":"DocumentReference","path":"type.coding[0].code","type":"code","system":"http://loinc.org"}},
        {"key":"date","label":"Note Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"date","type":"instant"}},
        {"key":"author","label":"Author","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"DocumentReference","path":"author[0].reference","type":"reference"}},
        {"key":"noteTitle","label":"Title","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"description","type":"string"}}
      ]
    },
    {
      "key": "soap-sections",
      "title": "Clinical Note Content (SOAP)",
      "columns": 1,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"subjective","label":"Subjective","type":"textarea","required":false,"colSpan":1,"placeholder":"Chief complaint, history of present illness...","fhirMapping":{"resource":"Composition","path":"section[0].text.div","type":"string"}},
        {"key":"objective","label":"Objective","type":"textarea","required":false,"colSpan":1,"placeholder":"Physical examination findings, vitals...","fhirMapping":{"resource":"Composition","path":"section[1].text.div","type":"string"}},
        {"key":"assessment","label":"Assessment","type":"textarea","required":false,"colSpan":1,"placeholder":"Diagnoses, clinical impressions...","fhirMapping":{"resource":"Composition","path":"section[2].text.div","type":"string"}},
        {"key":"plan","label":"Plan","type":"textarea","required":false,"colSpan":1,"placeholder":"Treatment plan, follow-up...","fhirMapping":{"resource":"Composition","path":"section[3].text.div","type":"string"}},
        {"key":"narrative","label":"Additional Narrative","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"content[0].attachment.data","type":"string"}}
      ]
    },
    {
      "key": "sign-off",
      "title": "Sign-off",
      "columns": 3,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {"key":"eSigned","label":"E-Signed","type":"boolean","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"authenticator.reference","type":"reference"}},
        {"key":"signedAt","label":"Signed At","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/signed-at].valueDateTime","type":"datetime"}},
        {"key":"signedBy","label":"Signed By","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/signed-by].valueString","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'visit-notes' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 4. CLAIMS → Claim
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]',
  field_config = '{
  "sections": [
    {
      "key": "claim-header",
      "title": "Claim Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}},
        {"key":"type","label":"Claim Type","type":"select","required":true,"colSpan":1,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"},{"value":"pharmacy","label":"Pharmacy"},{"value":"vision","label":"Vision"}],"fhirMapping":{"resource":"Claim","path":"type.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/claim-type"}},
        {"key":"use","label":"Use","type":"select","required":true,"colSpan":1,"options":[{"value":"claim","label":"Claim"},{"value":"preauthorization","label":"Pre-authorization"},{"value":"predetermination","label":"Pre-determination"}],"fhirMapping":{"resource":"Claim","path":"use","type":"code"}},
        {"key":"created","label":"Created Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"created","type":"dateTime"}},
        {"key":"billablePeriodStart","label":"Service From","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"billablePeriod.start","type":"date"}},
        {"key":"billablePeriodEnd","label":"Service To","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"billablePeriod.end","type":"date"}},
        {"key":"provider","label":"Billing Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"provider.reference","type":"reference"}},
        {"key":"insurer","label":"Insurer","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"id","searchable":true},"fhirMapping":{"resource":"Claim","path":"insurer.reference","type":"reference"}},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"stat","label":"Immediate"},{"value":"normal","label":"Normal"},{"value":"deferred","label":"Deferred"}],"fhirMapping":{"resource":"Claim","path":"priority.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/processpriority"}},
        {"key":"total","label":"Total Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"total.value","type":"decimal"}},
        {"key":"facilityReference","label":"Facility","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/facilities","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"facility.reference","type":"reference"}}
      ]
    },
    {
      "key": "claim-diagnosis",
      "title": "Diagnosis",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"diagnosis1","label":"Primary Diagnosis (ICD-10)","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[0].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"diagnosis2","label":"Secondary Diagnosis","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[1].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"diagnosis3","label":"Tertiary Diagnosis","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[2].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"diagnosis4","label":"Quaternary Diagnosis","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[3].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}}
      ]
    },
    {
      "key": "claim-items",
      "title": "Service Line Items",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"itemCpt","label":"CPT Code","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].productOrService.coding[0].code","type":"code","system":"http://www.ama-assn.org/go/cpt"}},
        {"key":"itemServiceDate","label":"Service Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].servicedDate","type":"date"}},
        {"key":"itemUnitPrice","label":"Unit Price","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].unitPrice.value","type":"decimal"}},
        {"key":"itemQuantity","label":"Quantity","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].quantity.value","type":"decimal"}},
        {"key":"itemNet","label":"Net Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].net.value","type":"decimal"}},
        {"key":"itemModifier","label":"Modifier","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].modifier[0].coding[0].code","type":"code"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'claims' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 5. CLAIM SUBMISSIONS → Claim (submission view)
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]',
  field_config = '{
  "sections": [
    {
      "key": "submission-details",
      "title": "Submission Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}},
        {"key":"created","label":"Submission Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"created","type":"dateTime"}},
        {"key":"trackingNumber","label":"Tracking Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"identifier[0].value","type":"string"}},
        {"key":"insurer","label":"Insurer","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"id","searchable":true},"fhirMapping":{"resource":"Claim","path":"insurer.reference","type":"reference"}},
        {"key":"total","label":"Total Charge","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"total.value","type":"decimal"}},
        {"key":"type","label":"Claim Type","type":"select","required":false,"colSpan":1,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"}],"fhirMapping":{"resource":"Claim","path":"type.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/claim-type"}},
        {"key":"provider","label":"Billing Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"provider.reference","type":"reference"}},
        {"key":"billablePeriodStart","label":"Service From","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"billablePeriod.start","type":"date"}},
        {"key":"billablePeriodEnd","label":"Service To","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"billablePeriod.end","type":"date"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'claim-submissions' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 6. CLAIM DENIALS → ClaimResponse
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"ClaimResponse","patientSearchParam":"patient"}]',
  field_config = '{
  "sections": [
    {
      "key": "denial-header",
      "title": "Denial Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"ClaimResponse","path":"status","type":"code"}},
        {"key":"outcome","label":"Outcome","type":"select","required":true,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"ClaimResponse","path":"outcome","type":"code"}},
        {"key":"disposition","label":"Disposition","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"disposition","type":"string"}},
        {"key":"created","label":"Response Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"created","type":"dateTime"}},
        {"key":"insurer","label":"Insurer","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"id","searchable":true},"fhirMapping":{"resource":"ClaimResponse","path":"insurer.reference","type":"reference"}},
        {"key":"request","label":"Original Claim","type":"lookup","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"reference"}},
        {"key":"preAuthRef","label":"Pre-Auth Reference","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"preAuthRef","type":"string"}},
        {"key":"use","label":"Use","type":"select","required":false,"colSpan":1,"options":[{"value":"claim","label":"Claim"},{"value":"preauthorization","label":"Pre-authorization"},{"value":"predetermination","label":"Pre-determination"}],"fhirMapping":{"resource":"ClaimResponse","path":"use","type":"code"}},
        {"key":"type","label":"Claim Type","type":"select","required":false,"colSpan":1,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"}],"fhirMapping":{"resource":"ClaimResponse","path":"type.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/claim-type"}}
      ]
    },
    {
      "key": "denial-adjudication",
      "title": "Adjudication Summary",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"totalSubmitted","label":"Submitted Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"total[0].amount.value","type":"decimal"}},
        {"key":"totalBenefit","label":"Benefit Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"total[1].amount.value","type":"decimal"}},
        {"key":"paymentAmount","label":"Payment Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"payment.amount.value","type":"decimal"}},
        {"key":"paymentDate","label":"Payment Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"payment.date","type":"date"}},
        {"key":"adjustmentAmount","label":"Adjustment","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"payment.adjustment.value","type":"decimal"}},
        {"key":"adjustmentReason","label":"Adjustment Reason","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"payment.adjustmentReason.text","type":"string"}}
      ]
    },
    {
      "key": "denial-notes",
      "title": "Process Notes",
      "columns": 1,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {"key":"processNote","label":"Process Note","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"processNote[0].text","type":"string"}},
        {"key":"errorCode","label":"Error Code","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ClaimResponse","path":"error[0].code.text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'claim-denials' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 7. ERA / REMITTANCE → ExplanationOfBenefit
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"ExplanationOfBenefit","patientSearchParam":"patient"}]',
  field_config = '{
  "sections": [
    {
      "key": "eob-header",
      "title": "ERA / Remittance Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"ExplanationOfBenefit","path":"status","type":"code"}},
        {"key":"outcome","label":"Outcome","type":"select","required":true,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"ExplanationOfBenefit","path":"outcome","type":"code"}},
        {"key":"type","label":"Claim Type","type":"select","required":false,"colSpan":1,"options":[{"value":"professional","label":"Professional"},{"value":"institutional","label":"Institutional"},{"value":"oral","label":"Oral/Dental"},{"value":"pharmacy","label":"Pharmacy"}],"fhirMapping":{"resource":"ExplanationOfBenefit","path":"type.coding[0].code","type":"code","system":"http://terminology.hl7.org/CodeSystem/claim-type"}},
        {"key":"use","label":"Use","type":"select","required":false,"colSpan":1,"options":[{"value":"claim","label":"Claim"},{"value":"preauthorization","label":"Pre-authorization"},{"value":"predetermination","label":"Pre-determination"}],"fhirMapping":{"resource":"ExplanationOfBenefit","path":"use","type":"code"}},
        {"key":"created","label":"Created Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"created","type":"dateTime"}},
        {"key":"insurer","label":"Payer","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"id","searchable":true},"fhirMapping":{"resource":"ExplanationOfBenefit","path":"insurer.reference","type":"reference"}},
        {"key":"provider","label":"Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"ExplanationOfBenefit","path":"provider.reference","type":"reference"}},
        {"key":"disposition","label":"Disposition","type":"text","required":false,"colSpan":2,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"disposition","type":"string"}},
        {"key":"billablePeriodStart","label":"Service From","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"billablePeriod.start","type":"date"}},
        {"key":"billablePeriodEnd","label":"Service To","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"billablePeriod.end","type":"date"}}
      ]
    },
    {
      "key": "eob-payment",
      "title": "Payment Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"paymentDate","label":"Check/EFT Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.date","type":"date"}},
        {"key":"paymentAmount","label":"Total Paid","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.amount.value","type":"decimal"}},
        {"key":"paymentIdentifier","label":"Check/EFT Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.identifier.value","type":"string"}},
        {"key":"paymentType","label":"Payment Type","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.type.text","type":"string"}},
        {"key":"paymentAdjustment","label":"Adjustment","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.adjustment.value","type":"decimal"}},
        {"key":"paymentAdjustmentReason","label":"Adjustment Reason","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"payment.adjustmentReason.text","type":"string"}}
      ]
    },
    {
      "key": "eob-totals",
      "title": "Totals",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"totalSubmitted","label":"Total Submitted","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"total[0].amount.value","type":"decimal"}},
        {"key":"totalBenefit","label":"Total Benefit","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"total[1].amount.value","type":"decimal"}},
        {"key":"processNote","label":"Process Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"ExplanationOfBenefit","path":"processNote[0].text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'era-remittance' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 8. PAYMENTS → PaymentReconciliation
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"PaymentReconciliation","patientSearchParam":""}]',
  field_config = '{
  "sections": [
    {
      "key": "payment-header",
      "title": "Payment Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"status","type":"code"}},
        {"key":"outcome","label":"Outcome","type":"select","required":false,"colSpan":1,"options":[{"value":"queued","label":"Queued"},{"value":"complete","label":"Complete"},{"value":"error","label":"Error"},{"value":"partial","label":"Partial"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"outcome","type":"code"}},
        {"key":"created","label":"Created Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"created","type":"dateTime"}},
        {"key":"paymentDate","label":"Payment Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentDate","type":"date"}},
        {"key":"paymentAmount","label":"Payment Amount","type":"number","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentAmount.value","type":"decimal"}},
        {"key":"paymentIssuer","label":"Payment Issuer","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIssuer.display","type":"string"}},
        {"key":"paymentIdentifier","label":"Reference Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"paymentIdentifier.value","type":"string"}},
        {"key":"periodStart","label":"Period Start","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"period.start","type":"date"}},
        {"key":"periodEnd","label":"Period End","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"period.end","type":"date"}}
      ]
    },
    {
      "key": "payment-detail",
      "title": "Payment Details",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"detailType","label":"Payment Method","type":"select","required":false,"colSpan":1,"options":[{"value":"payment","label":"Payment"},{"value":"adjustment","label":"Adjustment"},{"value":"advance","label":"Advance"}],"fhirMapping":{"resource":"PaymentReconciliation","path":"detail[0].type.coding[0].code","type":"code"}},
        {"key":"detailAmount","label":"Detail Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"detail[0].amount.value","type":"decimal"}},
        {"key":"detailDate","label":"Detail Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentReconciliation","path":"detail[0].date","type":"date"}},
        {"key":"processNote","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"PaymentReconciliation","path":"processNote[0].text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'payments' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 9. STATEMENTS → Invoice
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Invoice","patientSearchParam":"subject"}]',
  field_config = '{
  "sections": [
    {
      "key": "statement-header",
      "title": "Statement Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"issued","label":"Issued"},{"value":"balanced","label":"Balanced"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Invoice","path":"status","type":"code"}},
        {"key":"type","label":"Invoice Type","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"type.text","type":"string"}},
        {"key":"date","label":"Statement Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"date","type":"dateTime"}},
        {"key":"recipient","label":"Recipient","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/patients","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Invoice","path":"recipient.reference","type":"reference"}},
        {"key":"issuer","label":"Issuer","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"issuer.display","type":"string"}},
        {"key":"identifier","label":"Invoice Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"identifier[0].value","type":"string"}}
      ]
    },
    {
      "key": "statement-totals",
      "title": "Financial Summary",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"totalNet","label":"Total Net","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"totalNet.value","type":"decimal"}},
        {"key":"totalGross","label":"Total Gross","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"totalGross.value","type":"decimal"}},
        {"key":"paymentTerms","label":"Payment Terms","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"Invoice","path":"paymentTerms","type":"string"}},
        {"key":"note","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Invoice","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'statements' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 10. EDUCATION → Communication (category=education)
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Communication","patientSearchParam":"subject"}]',
  field_config = '{
  "sections": [
    {
      "key": "education-details",
      "title": "Patient Education",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"preparation","label":"Preparation"},{"value":"in-progress","label":"In Progress"},{"value":"not-done","label":"Not Done"},{"value":"on-hold","label":"On Hold"},{"value":"completed","label":"Completed"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Communication","path":"status","type":"code"}},
        {"key":"topic","label":"Topic / Title","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"topic.text","type":"string"}},
        {"key":"category","label":"Category","type":"select","required":false,"colSpan":1,"options":[{"value":"education","label":"Education"},{"value":"handout","label":"Handout"},{"value":"video","label":"Video"},{"value":"verbal","label":"Verbal Counseling"},{"value":"online","label":"Online Resource"}],"fhirMapping":{"resource":"Communication","path":"category[0].coding[0].code","type":"code"}},
        {"key":"sent","label":"Date Provided","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"sent","type":"dateTime"}},
        {"key":"medium","label":"Delivery Method","type":"select","required":false,"colSpan":1,"options":[{"value":"written","label":"Written"},{"value":"verbal","label":"Verbal"},{"value":"electronic","label":"Electronic"},{"value":"video","label":"Video"}],"fhirMapping":{"resource":"Communication","path":"medium[0].coding[0].code","type":"code"}},
        {"key":"sender","label":"Educator","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Communication","path":"sender.reference","type":"reference"}},
        {"key":"payload","label":"Content / Summary","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Communication","path":"payload[0].contentString","type":"string"}},
        {"key":"reasonCode","label":"Reason / Condition","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"reasonCode[0].text","type":"string"}},
        {"key":"language","label":"Language","type":"select","required":false,"colSpan":1,"options":[{"value":"en","label":"English"},{"value":"es","label":"Spanish"},{"value":"fr","label":"French"},{"value":"zh","label":"Chinese"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/education-language].valueCode","type":"code"}},
        {"key":"readingLevel","label":"Reading Level","type":"select","required":false,"colSpan":1,"options":[{"value":"easy","label":"Easy Read"},{"value":"standard","label":"Standard"},{"value":"advanced","label":"Advanced"}],"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/reading-level].valueCode","type":"code"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'education' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 11. MESSAGING → Communication (category=notification)
-- =====================================================
UPDATE tab_field_config SET
  fhir_resources = '[{"type":"Communication","patientSearchParam":"subject"}]',
  field_config = '{
  "sections": [
    {
      "key": "message-header",
      "title": "Message",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"preparation","label":"Draft"},{"value":"in-progress","label":"Sending"},{"value":"completed","label":"Sent"},{"value":"entered-in-error","label":"Error"}],"fhirMapping":{"resource":"Communication","path":"status","type":"code"}},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,"options":[{"value":"routine","label":"Routine"},{"value":"urgent","label":"Urgent"},{"value":"asap","label":"ASAP"},{"value":"stat","label":"STAT"}],"fhirMapping":{"resource":"Communication","path":"priority","type":"code"}},
        {"key":"category","label":"Category","type":"select","required":false,"colSpan":1,"options":[{"value":"notification","label":"Notification"},{"value":"reminder","label":"Reminder"},{"value":"instruction","label":"Instruction"},{"value":"alert","label":"Alert"}],"fhirMapping":{"resource":"Communication","path":"category[0].coding[0].code","type":"code"}},
        {"key":"subject","label":"Subject","type":"text","required":true,"colSpan":3,"fhirMapping":{"resource":"Communication","path":"topic.text","type":"string"}},
        {"key":"message","label":"Message Body","type":"textarea","required":true,"colSpan":3,"fhirMapping":{"resource":"Communication","path":"payload[0].contentString","type":"string"}},
        {"key":"sent","label":"Sent Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"sent","type":"dateTime"}},
        {"key":"received","label":"Received Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"received","type":"dateTime"}},
        {"key":"sender","label":"From","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Communication","path":"sender.reference","type":"reference"}},
        {"key":"recipient","label":"To","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/patients","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Communication","path":"recipient[0].reference","type":"reference"}},
        {"key":"inResponseTo","label":"In Response To","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"inResponseTo[0].reference","type":"reference"}}
      ]
    },
    {
      "key": "message-extensions",
      "title": "Metadata",
      "columns": 3,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {"key":"messageType","label":"Message Type","type":"select","required":false,"colSpan":1,"options":[{"value":"provider_to_patient","label":"Provider → Patient"},{"value":"patient_to_provider","label":"Patient → Provider"},{"value":"provider_to_provider","label":"Provider → Provider"}],"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/message-type].valueString","type":"string"}},
        {"key":"fromType","label":"Sender Type","type":"select","required":false,"colSpan":1,"options":[{"value":"provider","label":"Provider"},{"value":"patient","label":"Patient"},{"value":"system","label":"System"}],"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/from-type].valueString","type":"string"}},
        {"key":"readAt","label":"Read At","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/read-at].valueDateTime","type":"datetime"}},
        {"key":"readBy","label":"Read By","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/read-by].valueString","type":"string"}},
        {"key":"fromName","label":"From Name","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Communication","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/from-name].valueString","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'messaging' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- Also update the 12 existing V5 tabs to use object format for fhir_resources
-- =====================================================
UPDATE tab_field_config SET fhir_resources = '[{"type":"Patient","patientSearchParam":""},{"type":"RelatedPerson","patientSearchParam":"patient"}]' WHERE tab_key = 'demographics' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Observation","patientSearchParam":"subject"}]' WHERE tab_key = 'vitals' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"AllergyIntolerance","patientSearchParam":"patient"}]' WHERE tab_key = 'allergies' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"MedicationRequest","patientSearchParam":"patient"}]' WHERE tab_key = 'medications' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Immunization","patientSearchParam":"patient"}]' WHERE tab_key = 'immunizations' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Encounter","patientSearchParam":"subject"}]' WHERE tab_key = 'encounters' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Condition","patientSearchParam":"subject"}]' WHERE tab_key = 'problem-list' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Procedure","patientSearchParam":"subject"}]' WHERE tab_key = 'procedures' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"DiagnosticReport","patientSearchParam":"subject"},{"type":"Observation","patientSearchParam":"subject"}]' WHERE tab_key = 'lab-results' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Coverage","patientSearchParam":"beneficiary"}]' WHERE tab_key = 'insurance' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"DocumentReference","patientSearchParam":"subject"}]' WHERE tab_key = 'documents' AND practice_type_code = '*' AND org_id = '*';
UPDATE tab_field_config SET fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]' WHERE tab_key = 'billing' AND practice_type_code = '*' AND org_id = '*';

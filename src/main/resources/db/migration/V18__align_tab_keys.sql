-- V18: Align tab_field_config tab_keys with UI page.tsx switch cases
-- Fix: V5 used 'problems' but UI uses 'medicalproblems'
-- Fix: V5 used 'lab-results' but UI uses 'labs'
-- Fix: V17 tried to update 'problem-list' which never existed (V5 had 'problems')
-- Add: healthcareservices and relationships configs (missing from V5)
-- Add: documents features.fileUpload config

-- =====================================================
-- 1. Rename mismatched tab_keys
-- =====================================================

UPDATE tab_field_config SET tab_key = 'medicalproblems', updated_at = now()
WHERE tab_key = 'problems' AND practice_type_code = '*' AND org_id = '*';

UPDATE tab_field_config SET tab_key = 'labs', updated_at = now()
WHERE tab_key = 'lab-results' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 2. Apply fhir_resources update missed by V17
--    (V17 targeted 'problem-list' which didn't exist)
-- =====================================================

UPDATE tab_field_config
SET fhir_resources = '[{"type":"Condition","patientSearchParam":"subject"}]',
    updated_at = now()
WHERE tab_key = 'medicalproblems' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 3. Add missing tab configs: healthcareservices
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'healthcareservices', '*', '*',
  '[{"type":"HealthcareService","patientSearchParam":""}]',
  '{
    "sections": [
      {
        "key": "service-details",
        "title": "Service Information",
        "columns": 3,
        "collapsible": false,
        "collapsed": false,
        "fields": [
          {"key":"name","label":"Service Name","type":"text","required":true,"colSpan":1,"placeholder":"Service name","fhirMapping":{"resource":"HealthcareService","path":"name","type":"string"}},
          {"key":"type","label":"Service Type","type":"text","required":true,"colSpan":1,"placeholder":"Service type","fhirMapping":{"resource":"HealthcareService","path":"type[0].text","type":"string"}},
          {"key":"location","label":"Location","type":"text","required":true,"colSpan":1,"placeholder":"Location","fhirMapping":{"resource":"HealthcareService","path":"location[0].display","type":"string"}},
          {"key":"hoursOfOperation","label":"Hours of Operation","type":"text","required":true,"colSpan":1,"placeholder":"e.g. Mon-Fri 9am-5pm","fhirMapping":{"resource":"HealthcareService","path":"availableTime[0].description","type":"string"}},
          {"key":"description","label":"Description","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"HealthcareService","path":"comment","type":"string"}}
        ]
      }
    ]
  }',
  1
);

-- =====================================================
-- 4. Add missing tab configs: relationships
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'relationships', '*', '*',
  '[{"type":"RelatedPerson","patientSearchParam":"patient"}]',
  '{
    "sections": [
      {
        "key": "relationship-details",
        "title": "Relationship Information",
        "columns": 3,
        "collapsible": false,
        "collapsed": false,
        "fields": [
          {"key":"relatedPatientName","label":"Related Person Name","type":"text","required":true,"colSpan":1,"placeholder":"Full name","fhirMapping":{"resource":"RelatedPerson","path":"name[0].text","type":"string"}},
          {"key":"relationshipType","label":"Relationship Type","type":"select","required":true,"colSpan":1,"options":[{"value":"spouse","label":"Spouse"},{"value":"parent","label":"Parent"},{"value":"child","label":"Child"},{"value":"sibling","label":"Sibling"},{"value":"guardian","label":"Guardian"},{"value":"friend","label":"Friend"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"RelatedPerson","path":"relationship[0].coding[0].code","type":"code"}},
          {"key":"emergencyContact","label":"Emergency Contact","type":"boolean","required":false,"colSpan":1,"fhirMapping":{"resource":"RelatedPerson","path":"extension[url=http://ciyex.com/fhir/emergency-contact].valueBoolean","type":"boolean"}},
          {"key":"phoneNumber","label":"Phone","type":"phone","required":false,"colSpan":1,"fhirMapping":{"resource":"RelatedPerson","path":"telecom[0].value","type":"string"}},
          {"key":"email","label":"Email","type":"email","required":false,"colSpan":1,"fhirMapping":{"resource":"RelatedPerson","path":"telecom[1].value","type":"string"}},
          {"key":"address","label":"Address","type":"text","required":false,"colSpan":1,"placeholder":"Address","fhirMapping":{"resource":"RelatedPerson","path":"address[0].text","type":"string"}},
          {"key":"active","label":"Active","type":"boolean","required":false,"colSpan":1,"fhirMapping":{"resource":"RelatedPerson","path":"active","type":"boolean"}},
          {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,"fhirMapping":{"resource":"RelatedPerson","path":"extension[url=http://ciyex.com/fhir/notes].valueString","type":"string"}}
        ]
      }
    ]
  }',
  1
);

-- =====================================================
-- 5. Update documents field_config with fileUpload features
-- =====================================================

UPDATE tab_field_config
SET field_config = '{
  "features": {
    "fileUpload": {
      "enabled": true,
      "dragDrop": true,
      "preview": true,
      "maxSizeMB": 10,
      "allowedTypes": ["pdf","jpg","jpeg","png","gif","docx","xlsx","txt","csv","zip"],
      "uploadEndpoint": "/api/documents/upload",
      "downloadEndpoint": "/api/documents/upload/{id}/download"
    }
  },
  "sections": [
    {
      "key": "document-details",
      "title": "Document Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"title","label":"Document Title","type":"text","required":true,"colSpan":1,"placeholder":"Document title","fhirMapping":{"resource":"DocumentReference","path":"description","type":"string"}},
        {"key":"category","label":"Category","type":"select","required":true,"colSpan":1,"options":[{"value":"clinical-note","label":"Clinical Note"},{"value":"discharge-summary","label":"Discharge Summary"},{"value":"lab-report","label":"Lab Report"},{"value":"imaging","label":"Imaging Report"},{"value":"consent","label":"Consent Form"},{"value":"referral","label":"Referral Letter"},{"value":"insurance","label":"Insurance Document"},{"value":"identification","label":"Identification"},{"value":"prescription","label":"Prescription"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"DocumentReference","path":"category[0].text","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"current","label":"Current"},{"value":"superseded","label":"Superseded"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"status","type":"code"}},
        {"key":"date","label":"Document Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"date","type":"datetime"}},
        {"key":"author","label":"Author","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"DocumentReference","path":"author[0].display","type":"string"}},
        {"key":"file","label":"Attachment","type":"file","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"content[0].attachment.url","type":"string"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'documents' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 6. Update encounters field_config with rowLink + badge colors
-- =====================================================

UPDATE tab_field_config
SET field_config = '{
  "features": {
    "rowLink": {
      "urlTemplate": "/patients/{patientId}/encounters/{id}"
    }
  },
  "sections": [
    {
      "key": "encounter-details",
      "title": "Encounter Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"type","label":"Encounter Type","type":"select","required":true,"colSpan":1,"options":[{"value":"AMB","label":"Ambulatory"},{"value":"EMER","label":"Emergency"},{"value":"IMP","label":"Inpatient"},{"value":"OBSENC","label":"Observation"},{"value":"HH","label":"Home Health"},{"value":"VR","label":"Virtual"}],"fhirMapping":{"resource":"Encounter","path":"class.code","type":"code"}},
        {"key":"reason","label":"Reason for Visit","type":"text","required":false,"colSpan":1,"placeholder":"Chief complaint","fhirMapping":{"resource":"Encounter","path":"reasonCode[0].text","type":"string"}},
        {"key":"provider","label":"Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Encounter","path":"participant[0].individual.reference","type":"reference"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"badgeColors":{"planned":"bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300","arrived":"bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300","in-progress":"bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300","finished":"bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300","cancelled":"bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300"},"options":[{"value":"planned","label":"Planned"},{"value":"arrived","label":"Arrived"},{"value":"in-progress","label":"In Progress"},{"value":"finished","label":"Finished"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"Encounter","path":"status","type":"code"}},
        {"key":"startDate","label":"Start Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Encounter","path":"period.start","type":"datetime"}},
        {"key":"endDate","label":"End Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Encounter","path":"period.end","type":"datetime"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'encounters' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 7. Update insurance field_config with badge colors
-- =====================================================

UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "insurance-details",
      "title": "Insurance Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"payerName","label":"Insurance Company","type":"lookup","required":true,"colSpan":1,"lookupConfig":{"endpoint":"/api/insurance-companies","displayField":"name","valueField":"id","searchable":true},"fhirMapping":{"resource":"Coverage","path":"payor[0].display","type":"reference"}},
        {"key":"planName","label":"Plan Name","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"class[0].value","type":"string"}},
        {"key":"groupNumber","label":"Group Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"class[1].value","type":"string"}},
        {"key":"subscriberId","label":"Subscriber ID","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"subscriberId","type":"string"}},
        {"key":"subscriberName","label":"Subscriber Name","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"subscriber.display","type":"string"}},
        {"key":"relationship","label":"Relationship to Subscriber","type":"select","required":false,"colSpan":1,"options":[{"value":"self","label":"Self"},{"value":"spouse","label":"Spouse"},{"value":"child","label":"Child"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Coverage","path":"relationship.coding[0].code","type":"code"}},
        {"key":"coverageType","label":"Coverage Order","type":"select","required":false,"colSpan":1,"options":[{"value":"primary","label":"Primary"},{"value":"secondary","label":"Secondary"},{"value":"tertiary","label":"Tertiary"}],"badgeColors":{"primary":"bg-blue-100 text-blue-700","secondary":"bg-purple-100 text-purple-700","tertiary":"bg-orange-100 text-orange-700"},"fhirMapping":{"resource":"Coverage","path":"order","type":"code"}},
        {"key":"policyNumber","label":"Policy Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"identifier[0].value","type":"string"}},
        {"key":"effectiveStart","label":"Effective From","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"period.start","type":"date"}},
        {"key":"effectiveEnd","label":"Effective To","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"period.end","type":"date"}},
        {"key":"copay","label":"Copay Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"costToBeneficiary[0].valueQuantity.value","type":"quantity","unit":"USD"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"badgeColors":{"active":"bg-green-100 text-green-700","cancelled":"bg-red-100 text-red-700","entered-in-error":"bg-gray-100 text-gray-700"},"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Coverage","path":"status","type":"code"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'insurance' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 8. Add missing tab configs: history (QuestionnaireResponse)
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'history', '*', '*',
  '[{"type":"QuestionnaireResponse","patientSearchParam":"subject"}]',
  '{
    "sections": [
      {
        "key": "general-history",
        "title": "General History",
        "columns": 3,
        "collapsible": true,
        "collapsed": false,
        "fields": [
          {"key":"smokingStatus","label":"Smoking Status","type":"select","required":false,"colSpan":1,"options":[{"value":"current","label":"Current Smoker"},{"value":"former","label":"Former Smoker"},{"value":"never","label":"Never Smoker"}],"fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''smoking-status'').answer[0].valueCoding.code","type":"code"}},
          {"key":"alcoholUse","label":"Alcohol Use","type":"select","required":false,"colSpan":1,"options":[{"value":"none","label":"None"},{"value":"social","label":"Social"},{"value":"moderate","label":"Moderate"},{"value":"heavy","label":"Heavy"}],"fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''alcohol-use'').answer[0].valueCoding.code","type":"code"}},
          {"key":"exerciseFrequency","label":"Exercise","type":"select","required":false,"colSpan":1,"options":[{"value":"none","label":"None"},{"value":"occasional","label":"Occasional"},{"value":"regular","label":"Regular"},{"value":"daily","label":"Daily"}],"fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''exercise'').answer[0].valueCoding.code","type":"code"}},
          {"key":"additionalHistory","label":"Additional History","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''additional-notes'').answer[0].valueString","type":"string"}}
        ]
      },
      {
        "key": "family-history",
        "title": "Family History",
        "columns": 2,
        "collapsible": true,
        "collapsed": true,
        "fields": [
          {"key":"fatherHistory","label":"Father","type":"text","required":false,"colSpan":1,"placeholder":"Known conditions","fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''father-history'').answer[0].valueString","type":"string"}},
          {"key":"motherHistory","label":"Mother","type":"text","required":false,"colSpan":1,"placeholder":"Known conditions","fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''mother-history'').answer[0].valueString","type":"string"}},
          {"key":"siblingsHistory","label":"Siblings","type":"text","required":false,"colSpan":1,"placeholder":"Known conditions","fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''siblings-history'').answer[0].valueString","type":"string"}},
          {"key":"offspringHistory","label":"Offspring","type":"text","required":false,"colSpan":1,"placeholder":"Known conditions","fhirMapping":{"resource":"QuestionnaireResponse","path":"item.where(linkId=''offspring-history'').answer[0].valueString","type":"string"}}
        ]
      }
    ]
  }',
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config, fhir_resources = EXCLUDED.fhir_resources, updated_at = now();

-- =====================================================
-- 9. Add missing tab configs: issues (read-only aggregator)
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'issues', '*', '*',
  '[{"type":"Condition","patientSearchParam":"subject"},{"type":"AllergyIntolerance","patientSearchParam":"patient"},{"type":"MedicationRequest","patientSearchParam":"patient"}]',
  '{
    "sections": [
      {
        "key": "issues-overview",
        "title": "Active Issues",
        "columns": 3,
        "fields": [
          {"key":"conditionName","label":"Issue","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Condition","path":"code.text","type":"string"}},
          {"key":"status","label":"Status","type":"select","required":false,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"inactive","label":"Inactive"},{"value":"resolved","label":"Resolved"}],"badgeColors":{"active":"bg-red-100 text-red-700","inactive":"bg-gray-100 text-gray-700","resolved":"bg-green-100 text-green-700"},"fhirMapping":{"resource":"Condition","path":"clinicalStatus.coding[0].code","type":"code"}},
          {"key":"onsetDate","label":"Onset Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Condition","path":"onsetDateTime","type":"date"}}
        ]
      }
    ]
  }',
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config, fhir_resources = EXCLUDED.fhir_resources, updated_at = now();

-- =====================================================
-- 10. Add missing tab configs: payment (PaymentNotice)
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'payment', '*', '*',
  '[{"type":"PaymentNotice","patientSearchParam":""}]',
  '{
    "sections": [
      {
        "key": "payment-methods",
        "title": "Payment Methods",
        "columns": 3,
        "fields": [
          {"key":"cardType","label":"Card Type","type":"select","required":true,"colSpan":1,"options":[{"value":"visa","label":"Visa"},{"value":"mastercard","label":"Mastercard"},{"value":"amex","label":"American Express"},{"value":"discover","label":"Discover"}],"fhirMapping":{"resource":"PaymentNotice","path":"payment.identifier[0].type.text","type":"string"}},
          {"key":"lastFourDigits","label":"Last 4 Digits","type":"text","required":true,"colSpan":1,"placeholder":"XXXX","fhirMapping":{"resource":"PaymentNotice","path":"payment.identifier[0].value","type":"string"}},
          {"key":"expirationDate","label":"Expiration Date","type":"text","required":true,"colSpan":1,"placeholder":"MM/YY","fhirMapping":{"resource":"PaymentNotice","path":"payment.identifier[1].value","type":"string"}},
          {"key":"cardholderName","label":"Cardholder Name","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"PaymentNotice","path":"recipient.display","type":"string"}},
          {"key":"isDefault","label":"Default Card","type":"boolean","required":false,"colSpan":1,"fhirMapping":{"resource":"PaymentNotice","path":"extension[url=http://ciyex.com/fhir/default-payment].valueBoolean","type":"boolean"}},
          {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Inactive"}],"badgeColors":{"active":"bg-green-100 text-green-700","cancelled":"bg-gray-100 text-gray-700"},"fhirMapping":{"resource":"PaymentNotice","path":"status","type":"code"}}
        ]
      }
    ]
  }',
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config, fhir_resources = EXCLUDED.fhir_resources, updated_at = now();

-- =====================================================
-- 11. Add missing tab configs: report
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'report', '*', '*',
  '[{"type":"MeasureReport","patientSearchParam":"subject"}]',
  '{
    "sections": [
      {
        "key": "report-details",
        "title": "Reports",
        "columns": 3,
        "fields": [
          {"key":"reportType","label":"Report Type","type":"select","required":true,"colSpan":1,"options":[{"value":"clinical-summary","label":"Clinical Summary"},{"value":"visit-summary","label":"Visit Summary"},{"value":"lab-summary","label":"Lab Summary"},{"value":"medication-list","label":"Medication List"}],"fhirMapping":{"resource":"MeasureReport","path":"measure","type":"string"}},
          {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"complete","label":"Complete"},{"value":"pending","label":"Pending"},{"value":"error","label":"Error"}],"badgeColors":{"complete":"bg-green-100 text-green-700","pending":"bg-yellow-100 text-yellow-700","error":"bg-red-100 text-red-700"},"fhirMapping":{"resource":"MeasureReport","path":"status","type":"code"}},
          {"key":"date","label":"Report Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"MeasureReport","path":"date","type":"date"}}
        ]
      }
    ]
  }',
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config, fhir_resources = EXCLUDED.fhir_resources, updated_at = now();

-- =====================================================
-- 12. Update billing to show only claims/payments (RCM handles the rest)
-- =====================================================

UPDATE tab_field_config
SET fhir_resources = '[{"type":"Claim","patientSearchParam":"patient"}]',
    field_config = '{
  "sections": [
    {
      "key": "claims",
      "title": "Claims",
      "columns": 3,
      "fields": [
        {"key":"serviceDate","label":"Service Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].servicedDate","type":"date"}},
        {"key":"cptCode","label":"CPT Code","type":"coded","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].productOrService.coding[0].code","type":"code","system":"http://www.ama-assn.org/go/cpt"}},
        {"key":"diagnosisCode","label":"Diagnosis (ICD-10)","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[0].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"provider","label":"Rendering Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"provider.display","type":"string"}},
        {"key":"amount","label":"Charge Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].unitPrice.value","type":"quantity","unit":"USD"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"badgeColors":{"active":"bg-blue-100 text-blue-700","cancelled":"bg-red-100 text-red-700","draft":"bg-gray-100 text-gray-700","entered-in-error":"bg-yellow-100 text-yellow-700"},"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'billing' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 13. Add transactions tab (alias to billing/claims view)
-- =====================================================

INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config, version)
VALUES (
  'transactions', '*', '*',
  '[{"type":"Claim","patientSearchParam":"patient"},{"type":"PaymentReconciliation","patientSearchParam":""}]',
  '{
    "sections": [
      {
        "key": "transactions",
        "title": "Transactions",
        "columns": 3,
        "fields": [
          {"key":"serviceDate","label":"Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].servicedDate","type":"date"}},
          {"key":"description","label":"Description","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].productOrService.text","type":"string"}},
          {"key":"amount","label":"Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].unitPrice.value","type":"quantity","unit":"USD"}},
          {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"badgeColors":{"active":"bg-blue-100 text-blue-700","cancelled":"bg-red-100 text-red-700","draft":"bg-gray-100 text-gray-700"},"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}}
        ]
      }
    ]
  }',
  1
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config, fhir_resources = EXCLUDED.fhir_resources, updated_at = now();

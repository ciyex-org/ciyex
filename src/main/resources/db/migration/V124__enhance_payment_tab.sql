-- V124: Enhance payment tab with claim linkage, DOS, and balance tracking
-- Adds claim number, date of service, charge amount fields
-- Reorganizes layout for better UX: claim info first, then payment details

UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "claim-info",
      "title": "Claim Reference",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"claimNumber","label":"Claim #","type":"text","required":false,"colSpan":1,"showInTable":true,"placeholder":"e.g. CLM-473774","fhirMapping":{"resource":"Invoice","path":"extension[url=http://ciyex.org/fhir/ext/claim-number].valueString","type":"string"}},
        {"key":"dateOfService","label":"Date of Service","type":"date","required":false,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"extension[url=http://ciyex.org/fhir/ext/dos].valueDate","type":"date"}},
        {"key":"chargeAmount","label":"Total Charges ($)","type":"number","required":false,"colSpan":1,"showInTable":false,"fhirMapping":{"resource":"Invoice","path":"totalGross.value","type":"decimal"}}
      ]
    },
    {
      "key": "payment-info",
      "title": "Payment Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"date","label":"Payment Date","type":"date","required":true,"colSpan":1,"showInTable":true,"defaultToday":true,"fhirMapping":{"resource":"Invoice","path":"date","type":"date"}},
        {"key":"amount","label":"Amount ($)","type":"number","required":true,"colSpan":1,"showInTable":true,"fhirMapping":{"resource":"Invoice","path":"totalNet.value","type":"decimal"}},
        {"key":"paymentType","label":"Payment Type","type":"select","required":true,"colSpan":1,"showInTable":true,"options":[{"value":"insurance","label":"Insurance Payment"},{"value":"patient_copay","label":"Patient Copay"},{"value":"patient_coinsurance","label":"Patient Coinsurance"},{"value":"patient_deductible","label":"Patient Deductible"},{"value":"patient_self_pay","label":"Patient Self-Pay"},{"value":"cash","label":"Cash"},{"value":"check","label":"Check"},{"value":"credit_card","label":"Credit Card"},{"value":"debit_card","label":"Debit Card"},{"value":"eft","label":"EFT/ACH"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Invoice","path":"paymentTerms","type":"string"}},
        {"key":"reference","label":"Reference / Check #","type":"text","required":false,"colSpan":1,"placeholder":"e.g. EOB-2026-001, CHK-1234","fhirMapping":{"resource":"Invoice","path":"identifier[0].value","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"draft","label":"Draft"},{"value":"issued","label":"Posted"},{"value":"balanced","label":"Balanced"},{"value":"cancelled","label":"Cancelled"}],"badgeColors":{"draft":"bg-gray-100 text-gray-700","issued":"bg-blue-100 text-blue-700","balanced":"bg-green-100 text-green-700","cancelled":"bg-red-100 text-red-700"},"fhirMapping":{"resource":"Invoice","path":"status","type":"code"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Invoice","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}',
    updated_at = now()
WHERE tab_key = 'payment'
  AND practice_type_code = '*'
  AND org_id = '*';

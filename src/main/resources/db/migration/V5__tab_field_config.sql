-- Tab field configuration: FHIR-mapped configurable fields per tab
-- Supports 3-level fallback: org-specific → practice-type → universal (*)

CREATE TABLE tab_field_config (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tab_key             VARCHAR(100) NOT NULL,
    practice_type_code  VARCHAR(100) NOT NULL DEFAULT '*',
    org_id              VARCHAR(100) NOT NULL DEFAULT '*',
    fhir_resources      JSONB NOT NULL DEFAULT '[]',
    field_config        JSONB NOT NULL,
    version             INT NOT NULL DEFAULT 1,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (tab_key, practice_type_code, org_id)
);

CREATE INDEX idx_tfc_tab_key ON tab_field_config(tab_key);
CREATE INDEX idx_tfc_practice ON tab_field_config(practice_type_code);
CREATE INDEX idx_tfc_org ON tab_field_config(org_id);

-- RLS
ALTER TABLE tab_field_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY tfc_tenant_policy ON tab_field_config
    USING (org_id = '*' OR org_id = current_setting('app.current_org', true));
ALTER TABLE tab_field_config FORCE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON tab_field_config TO app_user;

-- =====================================================
-- Universal Defaults (practice_type_code = '*')
-- =====================================================

-- 1. Demographics Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('demographics', '*', '*', '["Patient","RelatedPerson"]',
'{
  "sections": [
    {
      "key": "personal-info",
      "title": "Personal Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"firstName","label":"First Name","type":"text","required":true,"colSpan":1,"placeholder":"First name","fhirMapping":{"resource":"Patient","path":"name[0].given[0]","type":"string"},"validation":{"maxLength":100}},
        {"key":"lastName","label":"Last Name","type":"text","required":true,"colSpan":1,"placeholder":"Last name","fhirMapping":{"resource":"Patient","path":"name[0].family","type":"string"},"validation":{"maxLength":100}},
        {"key":"middleName","label":"Middle Name","type":"text","required":false,"colSpan":1,"placeholder":"Middle name","fhirMapping":{"resource":"Patient","path":"name[0].given[1]","type":"string"}},
        {"key":"preferredName","label":"Preferred Name","type":"text","required":false,"colSpan":1,"placeholder":"Preferred name","fhirMapping":{"resource":"Patient","path":"name[1].given[0]","type":"string"}},
        {"key":"dateOfBirth","label":"Date of Birth","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"birthDate","type":"date"}},
        {"key":"gender","label":"Sex at Birth","type":"select","required":true,"colSpan":1,"options":[{"value":"male","label":"Male"},{"value":"female","label":"Female"},{"value":"other","label":"Other"},{"value":"unknown","label":"Unknown"}],"fhirMapping":{"resource":"Patient","path":"gender","type":"code"}},
        {"key":"mrn","label":"Medical Record Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"identifier[0].value","type":"string"}},
        {"key":"ssn","label":"SSN","type":"text","required":false,"colSpan":1,"placeholder":"XXX-XX-XXXX","fhirMapping":{"resource":"Patient","path":"identifier[2].value","type":"string"},"validation":{"pattern":"^\\d{3}-?\\d{2}-?\\d{4}$"}},
        {"key":"maritalStatus","label":"Marital Status","type":"select","required":false,"colSpan":1,"options":[{"value":"S","label":"Single"},{"value":"M","label":"Married"},{"value":"D","label":"Divorced"},{"value":"W","label":"Widowed"},{"value":"L","label":"Legally Separated"},{"value":"P","label":"Domestic Partner"}],"fhirMapping":{"resource":"Patient","path":"maritalStatus.coding[0].code","type":"code"}},
        {"key":"race","label":"Race","type":"select","required":false,"colSpan":1,"options":[{"value":"2106-3","label":"White"},{"value":"2054-5","label":"Black or African American"},{"value":"2028-9","label":"Asian"},{"value":"1002-5","label":"American Indian or Alaska Native"},{"value":"2076-8","label":"Native Hawaiian or Other Pacific Islander"},{"value":"2131-1","label":"Other Race"}],"fhirMapping":{"resource":"Patient","path":"extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-race].extension[url=ombCategory].valueCoding.code","type":"code"}},
        {"key":"ethnicity","label":"Ethnicity","type":"select","required":false,"colSpan":1,"options":[{"value":"2135-2","label":"Hispanic or Latino"},{"value":"2186-5","label":"Not Hispanic or Latino"}],"fhirMapping":{"resource":"Patient","path":"extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity].extension[url=ombCategory].valueCoding.code","type":"code"}},
        {"key":"language","label":"Preferred Language","type":"select","required":false,"colSpan":1,"options":[{"value":"en","label":"English"},{"value":"es","label":"Spanish"},{"value":"fr","label":"French"},{"value":"zh","label":"Chinese"},{"value":"vi","label":"Vietnamese"},{"value":"ko","label":"Korean"},{"value":"ar","label":"Arabic"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Patient","path":"communication[0].language.coding[0].code","type":"code"}}
      ]
    },
    {
      "key": "contact-info",
      "title": "Contact Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"phoneNumber","label":"Mobile Phone","type":"phone","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"telecom[0].value","type":"string"}},
        {"key":"homePhone","label":"Home Phone","type":"phone","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"telecom[1].value","type":"string"}},
        {"key":"email","label":"Email Address","type":"email","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"telecom[2].value","type":"string"}},
        {"key":"address","label":"Address","type":"address","required":false,"colSpan":3,"fhirMapping":{"resource":"Patient","path":"address[0]","type":"address"}}
      ]
    },
    {
      "key": "emergency-contact",
      "title": "Emergency Contact",
      "columns": 3,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {"key":"emergencyName","label":"Contact Name","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"contact[0].name.text","type":"string"}},
        {"key":"emergencyRelationship","label":"Relationship","type":"select","required":false,"colSpan":1,"options":[{"value":"spouse","label":"Spouse"},{"value":"parent","label":"Parent"},{"value":"child","label":"Child"},{"value":"sibling","label":"Sibling"},{"value":"friend","label":"Friend"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"Patient","path":"contact[0].relationship[0].coding[0].code","type":"code"}},
        {"key":"emergencyPhone","label":"Phone","type":"phone","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"contact[0].telecom[0].value","type":"string"}}
      ]
    },
    {
      "key": "provider-info",
      "title": "Provider & Practice",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"assignedProvider","label":"Assigned Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Patient","path":"generalPractitioner[0].reference","type":"reference"}},
        {"key":"referringProvider","label":"Referring Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Patient","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/referring-provider].valueReference.reference","type":"reference"}},
        {"key":"status","label":"Patient Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"inactive","label":"Inactive"}],"fhirMapping":{"resource":"Patient","path":"active","type":"boolean"}}
      ]
    },
    {
      "key": "employer-info",
      "title": "Employer Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {"key":"employerName","label":"Employer Name","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/employer-name].valueString","type":"string"}},
        {"key":"employerPhone","label":"Employer Phone","type":"phone","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/employer-phone].valueString","type":"string"}},
        {"key":"employerAddress","label":"Employer Address","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Patient","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/employer-address].valueString","type":"string"}}
      ]
    }
  ]
}');

-- 2. Vitals Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('vitals', '*', '*', '["Observation"]',
'{
  "sections": [
    {
      "key": "measurements",
      "title": "Measurements",
      "columns": 4,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"weightKg","label":"Weight (kg)","type":"number","required":false,"colSpan":1,"placeholder":"kg","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''29463-7'').valueQuantity.value","type":"quantity","loincCode":"29463-7","unit":"kg"}},
        {"key":"heightCm","label":"Height (cm)","type":"number","required":false,"colSpan":1,"placeholder":"cm","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8302-2'').valueQuantity.value","type":"quantity","loincCode":"8302-2","unit":"cm"}},
        {"key":"bmi","label":"BMI","type":"computed","required":false,"colSpan":1,"computeExpression":"round(weightKg / ((heightCm/100) * (heightCm/100)), 1)","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''39156-5'').valueQuantity.value","type":"quantity","loincCode":"39156-5","unit":"kg/m2"}},
        {"key":"bpSystolic","label":"BP Systolic","type":"number","required":false,"colSpan":1,"placeholder":"mmHg","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8480-6'').valueQuantity.value","type":"quantity","loincCode":"8480-6","unit":"mmHg"}},
        {"key":"bpDiastolic","label":"BP Diastolic","type":"number","required":false,"colSpan":1,"placeholder":"mmHg","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8462-4'').valueQuantity.value","type":"quantity","loincCode":"8462-4","unit":"mmHg"}},
        {"key":"pulse","label":"Pulse","type":"number","required":false,"colSpan":1,"placeholder":"/min","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8867-4'').valueQuantity.value","type":"quantity","loincCode":"8867-4","unit":"/min"}},
        {"key":"respiration","label":"Respiration","type":"number","required":false,"colSpan":1,"placeholder":"/min","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''9279-1'').valueQuantity.value","type":"quantity","loincCode":"9279-1","unit":"/min"}},
        {"key":"temperatureC","label":"Temperature (°C)","type":"number","required":false,"colSpan":1,"placeholder":"°C","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8310-5'').valueQuantity.value","type":"quantity","loincCode":"8310-5","unit":"Cel"}},
        {"key":"oxygenSaturation","label":"O2 Saturation (%)","type":"number","required":false,"colSpan":1,"placeholder":"%","fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''2708-6'').valueQuantity.value","type":"quantity","loincCode":"2708-6","unit":"%"}}
      ]
    },
    {
      "key": "vitals-meta",
      "title": "Recording Info",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"recordedAt","label":"Date/Time","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Observation","path":"effectiveDateTime","type":"datetime"}},
        {"key":"signed","label":"E-Signed","type":"boolean","required":false,"colSpan":1,"fhirMapping":{"resource":"Observation","path":"status","type":"code"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"Observation","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}');

-- 3. Allergies Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('allergies', '*', '*', '["AllergyIntolerance"]',
'{
  "sections": [
    {
      "key": "allergy-details",
      "title": "Allergy Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"allergyName","label":"Allergen","type":"text","required":true,"colSpan":1,"placeholder":"Allergen name","fhirMapping":{"resource":"AllergyIntolerance","path":"code.text","type":"string"}},
        {"key":"status","label":"Clinical Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"inactive","label":"Inactive"},{"value":"resolved","label":"Resolved"}],"fhirMapping":{"resource":"AllergyIntolerance","path":"clinicalStatus.coding[0].code","type":"code"}},
        {"key":"severity","label":"Severity","type":"select","required":false,"colSpan":1,"options":[{"value":"mild","label":"Mild"},{"value":"moderate","label":"Moderate"},{"value":"severe","label":"Severe"}],"fhirMapping":{"resource":"AllergyIntolerance","path":"criticality","type":"code"}},
        {"key":"reaction","label":"Reaction","type":"text","required":false,"colSpan":1,"placeholder":"Describe reaction","fhirMapping":{"resource":"AllergyIntolerance","path":"reaction[0].manifestation[0].text","type":"string"}},
        {"key":"startDate","label":"Onset Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"AllergyIntolerance","path":"onsetDateTime","type":"date"}},
        {"key":"endDate","label":"End Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"AllergyIntolerance","path":"extension[url=http://ciyex.com/fhir/StructureDefinition/allergy-end-date].valueDate","type":"date"}},
        {"key":"comments","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"AllergyIntolerance","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}');

-- 4. Medications Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('medications', '*', '*', '["MedicationRequest"]',
'{
  "sections": [
    {
      "key": "medication-details",
      "title": "Medication Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"medicationName","label":"Medication Name","type":"text","required":true,"colSpan":1,"placeholder":"Medication name","fhirMapping":{"resource":"MedicationRequest","path":"medicationCodeableConcept.text","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"on-hold","label":"On Hold"},{"value":"cancelled","label":"Cancelled"},{"value":"completed","label":"Completed"},{"value":"stopped","label":"Stopped"},{"value":"draft","label":"Draft"}],"fhirMapping":{"resource":"MedicationRequest","path":"status","type":"code"}},
        {"key":"dosage","label":"Dosage","type":"text","required":false,"colSpan":1,"placeholder":"e.g. 500mg","fhirMapping":{"resource":"MedicationRequest","path":"dosageInstruction[0].text","type":"string"}},
        {"key":"instructions","label":"Instructions","type":"textarea","required":false,"colSpan":2,"placeholder":"Patient instructions","fhirMapping":{"resource":"MedicationRequest","path":"dosageInstruction[0].patientInstruction","type":"string"}},
        {"key":"prescribingDoctor","label":"Prescriber","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"MedicationRequest","path":"requester.display","type":"reference"}},
        {"key":"dateIssued","label":"Date Issued","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"MedicationRequest","path":"authoredOn","type":"datetime"}}
      ]
    }
  ]
}');

-- 5. Immunizations Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('immunizations', '*', '*', '["Immunization"]',
'{
  "sections": [
    {
      "key": "vaccine-info",
      "title": "Vaccine Information",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"cvxCode","label":"Vaccine (CVX)","type":"coded","required":true,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"vaccineCode.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/cvx"}},
        {"key":"dateTimeAdministered","label":"Date Administered","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"occurrenceDateTime","type":"datetime"}},
        {"key":"manufacturer","label":"Manufacturer","type":"text","required":false,"colSpan":1,"placeholder":"Manufacturer","fhirMapping":{"resource":"Immunization","path":"manufacturer.display","type":"string"}},
        {"key":"lotNumber","label":"Lot Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"lotNumber","type":"string"}},
        {"key":"expirationDate","label":"Expiration Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"expirationDate","type":"date"}},
        {"key":"route","label":"Route","type":"select","required":false,"colSpan":1,"options":[{"value":"IM","label":"Intramuscular"},{"value":"SC","label":"Subcutaneous"},{"value":"PO","label":"Oral"},{"value":"IN","label":"Intranasal"},{"value":"ID","label":"Intradermal"}],"fhirMapping":{"resource":"Immunization","path":"route.text","type":"string"}},
        {"key":"administrationSite","label":"Site","type":"select","required":false,"colSpan":1,"options":[{"value":"LA","label":"Left Arm"},{"value":"RA","label":"Right Arm"},{"value":"LT","label":"Left Thigh"},{"value":"RT","label":"Right Thigh"},{"value":"ORAL","label":"Oral"}],"fhirMapping":{"resource":"Immunization","path":"site.text","type":"string"}},
        {"key":"amountAdministered","label":"Dose Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"doseQuantity.value","type":"quantity"}},
        {"key":"administratorName","label":"Administrator","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Immunization","path":"performer[0].actor.display","type":"string"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Immunization","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}');

-- 6. Encounters Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('encounters', '*', '*', '["Encounter"]',
'{
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
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"planned","label":"Planned"},{"value":"arrived","label":"Arrived"},{"value":"in-progress","label":"In Progress"},{"value":"finished","label":"Finished"},{"value":"cancelled","label":"Cancelled"}],"fhirMapping":{"resource":"Encounter","path":"status","type":"code"}},
        {"key":"startDate","label":"Start Date","type":"datetime","required":true,"colSpan":1,"fhirMapping":{"resource":"Encounter","path":"period.start","type":"datetime"}},
        {"key":"endDate","label":"End Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Encounter","path":"period.end","type":"datetime"}}
      ]
    }
  ]
}');

-- 7. Problems Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('problems', '*', '*', '["Condition"]',
'{
  "sections": [
    {
      "key": "problem-details",
      "title": "Problem Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"conditionName","label":"Condition","type":"text","required":true,"colSpan":1,"placeholder":"Condition name","fhirMapping":{"resource":"Condition","path":"code.text","type":"string"}},
        {"key":"icdCode","label":"ICD-10 Code","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Condition","path":"code.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"clinicalStatus","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"recurrence","label":"Recurrence"},{"value":"relapse","label":"Relapse"},{"value":"inactive","label":"Inactive"},{"value":"remission","label":"Remission"},{"value":"resolved","label":"Resolved"}],"fhirMapping":{"resource":"Condition","path":"clinicalStatus.coding[0].code","type":"code"}},
        {"key":"severity","label":"Severity","type":"select","required":false,"colSpan":1,"options":[{"value":"mild","label":"Mild"},{"value":"moderate","label":"Moderate"},{"value":"severe","label":"Severe"}],"fhirMapping":{"resource":"Condition","path":"severity.coding[0].code","type":"code"}},
        {"key":"onsetDate","label":"Onset Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Condition","path":"onsetDateTime","type":"date"}},
        {"key":"abatementDate","label":"Resolved Date","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Condition","path":"abatementDateTime","type":"date"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":3,"fhirMapping":{"resource":"Condition","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}');

-- 8. Procedures Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('procedures', '*', '*', '["Procedure"]',
'{
  "sections": [
    {
      "key": "procedure-details",
      "title": "Procedure Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"procedureName","label":"Procedure","type":"text","required":true,"colSpan":1,"placeholder":"Procedure name","fhirMapping":{"resource":"Procedure","path":"code.text","type":"string"}},
        {"key":"cptCode","label":"CPT Code","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Procedure","path":"code.coding[0].code","type":"code","system":"http://www.ama-assn.org/go/cpt"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"preparation","label":"Preparation"},{"value":"in-progress","label":"In Progress"},{"value":"completed","label":"Completed"},{"value":"not-done","label":"Not Done"}],"fhirMapping":{"resource":"Procedure","path":"status","type":"code"}},
        {"key":"performedDate","label":"Date Performed","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"Procedure","path":"performedDateTime","type":"datetime"}},
        {"key":"performer","label":"Performer","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Procedure","path":"performer[0].actor.reference","type":"reference"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"Procedure","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}');

-- 9. Lab Results Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('lab-results', '*', '*', '["DiagnosticReport","Observation"]',
'{
  "sections": [
    {
      "key": "lab-info",
      "title": "Lab Report",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"testCode","label":"Test Code","type":"coded","required":true,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"code.coding[0].code","type":"code","system":"http://loinc.org"}},
        {"key":"testName","label":"Test Name","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"code.text","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"registered","label":"Registered"},{"value":"partial","label":"Partial"},{"value":"preliminary","label":"Preliminary"},{"value":"final","label":"Final"},{"value":"corrected","label":"Corrected"}],"fhirMapping":{"resource":"DiagnosticReport","path":"status","type":"code"}},
        {"key":"effectiveDate","label":"Collection Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"effectiveDateTime","type":"datetime"}},
        {"key":"issued","label":"Result Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"issued","type":"datetime"}},
        {"key":"conclusion","label":"Conclusion","type":"textarea","required":false,"colSpan":1,"fhirMapping":{"resource":"DiagnosticReport","path":"conclusion","type":"string"}}
      ]
    }
  ]
}');

-- 10. Insurance Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('insurance', '*', '*', '["Coverage"]',
'{
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
        {"key":"policyNumber","label":"Policy Number","type":"text","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"identifier[0].value","type":"string"}},
        {"key":"effectiveStart","label":"Effective From","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"period.start","type":"date"}},
        {"key":"effectiveEnd","label":"Effective To","type":"date","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"period.end","type":"date"}},
        {"key":"copay","label":"Copay Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"costToBeneficiary[0].valueQuantity.value","type":"quantity","unit":"USD"}},
        {"key":"deductible","label":"Deductible","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Coverage","path":"costToBeneficiary[1].valueQuantity.value","type":"quantity","unit":"USD"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Coverage","path":"status","type":"code"}}
      ]
    }
  ]
}');

-- 11. Documents Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('documents', '*', '*', '["DocumentReference"]',
'{
  "sections": [
    {
      "key": "document-details",
      "title": "Document Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"title","label":"Document Title","type":"text","required":true,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"description","type":"string"}},
        {"key":"type","label":"Document Type","type":"select","required":true,"colSpan":1,"options":[{"value":"clinical-note","label":"Clinical Note"},{"value":"discharge-summary","label":"Discharge Summary"},{"value":"lab-report","label":"Lab Report"},{"value":"imaging","label":"Imaging Report"},{"value":"consent","label":"Consent Form"},{"value":"referral","label":"Referral Letter"},{"value":"other","label":"Other"}],"fhirMapping":{"resource":"DocumentReference","path":"type.coding[0].code","type":"code"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"current","label":"Current"},{"value":"superseded","label":"Superseded"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"DocumentReference","path":"status","type":"code"}},
        {"key":"date","label":"Document Date","type":"datetime","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"date","type":"datetime"}},
        {"key":"author","label":"Author","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"DocumentReference","path":"author[0].reference","type":"reference"}},
        {"key":"file","label":"Attachment","type":"file","required":false,"colSpan":1,"fhirMapping":{"resource":"DocumentReference","path":"content[0].attachment.url","type":"string"}}
      ]
    }
  ]
}');

-- 12. Billing Tab
INSERT INTO tab_field_config (tab_key, practice_type_code, org_id, fhir_resources, field_config) VALUES
('billing', '*', '*', '["Claim"]',
'{
  "sections": [
    {
      "key": "billing-details",
      "title": "Billing Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"serviceDate","label":"Service Date","type":"date","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].servicedDate","type":"date"}},
        {"key":"cptCode","label":"CPT Code","type":"coded","required":true,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].productOrService.coding[0].code","type":"code","system":"http://www.ama-assn.org/go/cpt"}},
        {"key":"diagnosisCode","label":"Diagnosis (ICD-10)","type":"coded","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"diagnosis[0].diagnosisCodeableConcept.coding[0].code","type":"code","system":"http://hl7.org/fhir/sid/icd-10-cm"}},
        {"key":"provider","label":"Rendering Provider","type":"lookup","required":false,"colSpan":1,"lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},"fhirMapping":{"resource":"Claim","path":"provider.reference","type":"reference"}},
        {"key":"amount","label":"Charge Amount","type":"number","required":false,"colSpan":1,"fhirMapping":{"resource":"Claim","path":"item[0].unitPrice.value","type":"quantity","unit":"USD"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"options":[{"value":"active","label":"Active"},{"value":"cancelled","label":"Cancelled"},{"value":"draft","label":"Draft"},{"value":"entered-in-error","label":"Entered in Error"}],"fhirMapping":{"resource":"Claim","path":"status","type":"code"}}
      ]
    }
  ]
}');

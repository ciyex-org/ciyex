-- V42: Update Appointments field config to match scheduling modal + add showInTable to vitals

-- =====================================================
-- 1. APPOINTMENTS - Update field config
--    - Visit Type: combobox (configurable in Layout Settings, allows custom types)
--    - Provider: required
--    - Location: required
--    - showInTable on key fields for list view
--    - Reorder to match AppointmentModal
-- =====================================================
UPDATE tab_field_config SET
  field_config = '{
  "sections": [
    {
      "key": "appointment-details",
      "title": "Appointment Details",
      "columns": 3,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"appointmentType","label":"Visit Type","type":"combobox","required":true,"colSpan":1,
         "options":[{"value":"ROUTINE","label":"Routine"},{"value":"FOLLOWUP","label":"Follow-up"},{"value":"WALKIN","label":"Walk-in"},{"value":"CHECKUP","label":"Check-up"},{"value":"EMERGENCY","label":"Emergency"},{"value":"NEW_PATIENT","label":"New Patient"},{"value":"TELEHEALTH","label":"Telehealth"},{"value":"CONSULTATION","label":"Consultation"},{"value":"PROCEDURE","label":"Procedure"},{"value":"LAB_WORK","label":"Lab Work"},{"value":"IMAGING","label":"Imaging"},{"value":"VACCINATION","label":"Vaccination"},{"value":"ANNUAL_PHYSICAL","label":"Annual Physical"},{"value":"SICK_VISIT","label":"Sick Visit"},{"value":"WELLNESS","label":"Wellness Check"},{"value":"PRE_OP","label":"Pre-Op"},{"value":"POST_OP","label":"Post-Op"}],
         "fhirMapping":{"resource":"Appointment","path":"appointmentType.coding[0].code","type":"code"},
         "showInTable":true},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,
         "options":[{"value":"proposed","label":"Proposed"},{"value":"pending","label":"Pending"},{"value":"booked","label":"Booked"},{"value":"arrived","label":"Arrived"},{"value":"fulfilled","label":"Fulfilled"},{"value":"cancelled","label":"Cancelled"},{"value":"noshow","label":"No Show"},{"value":"checked-in","label":"Checked In"},{"value":"waitlist","label":"Waitlist"},{"value":"entered-in-error","label":"Entered in Error"}],
         "fhirMapping":{"resource":"Appointment","path":"status","type":"code"},
         "showInTable":true},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,
         "options":[{"value":"5","label":"Routine"},{"value":"1","label":"Urgent"},{"value":"2","label":"Callback Results"},{"value":"9","label":"Low Priority"}],
         "fhirMapping":{"resource":"Appointment","path":"priority","type":"unsignedInt"}},
        {"key":"start","label":"Start Date/Time","type":"datetime","required":true,"colSpan":1,
         "fhirMapping":{"resource":"Appointment","path":"start","type":"instant"},
         "showInTable":true},
        {"key":"end","label":"End Date/Time","type":"datetime","required":false,"colSpan":1,
         "fhirMapping":{"resource":"Appointment","path":"end","type":"instant"}},
        {"key":"minutesDuration","label":"Duration (min)","type":"number","required":false,"colSpan":1,
         "placeholder":"minutes",
         "fhirMapping":{"resource":"Appointment","path":"minutesDuration","type":"positiveInt"}},
        {"key":"reason","label":"Reason / Chief Complaint","type":"textarea","required":false,"colSpan":3,
         "placeholder":"Chief complaint or reason for visit",
         "fhirMapping":{"resource":"Appointment","path":"reasonCode[0].text","type":"string"}},
        {"key":"description","label":"Description","type":"text","required":false,"colSpan":3,
         "fhirMapping":{"resource":"Appointment","path":"description","type":"string"}},
        {"key":"patientInstruction","label":"Patient Instructions","type":"textarea","required":false,"colSpan":3,
         "fhirMapping":{"resource":"Appointment","path":"patientInstruction","type":"string"}},
        {"key":"comment","label":"Internal Notes","type":"textarea","required":false,"colSpan":3,
         "fhirMapping":{"resource":"Appointment","path":"comment","type":"string"}},
        {"key":"cancelationReason","label":"Cancellation Reason","type":"text","required":false,"colSpan":1,
         "fhirMapping":{"resource":"Appointment","path":"cancelationReason.text","type":"string"}}
      ]
    },
    {
      "key": "participants",
      "title": "Participants",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"patient","label":"Patient","type":"lookup","required":true,"colSpan":1,
         "lookupConfig":{"endpoint":"/api/patients","displayField":"name","valueField":"fhirId","searchable":true},
         "fhirMapping":{"resource":"Appointment","path":"participant[0].actor.reference","type":"reference"}},
        {"key":"provider","label":"Provider","type":"lookup","required":true,"colSpan":1,
         "lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},
         "fhirMapping":{"resource":"Appointment","path":"participant[1].actor.reference","type":"reference"},
         "showInTable":true},
        {"key":"location","label":"Location","type":"lookup","required":true,"colSpan":1,
         "lookupConfig":{"endpoint":"/api/locations","displayField":"name","valueField":"fhirId","searchable":true},
         "fhirMapping":{"resource":"Appointment","path":"participant[2].actor.reference","type":"reference"},
         "showInTable":true}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments' AND practice_type_code = '*' AND org_id = '*';

-- =====================================================
-- 2. VITALS - Add showInTable to key measurement fields
-- =====================================================
UPDATE tab_field_config SET
  field_config = '{
  "sections": [
    {
      "key": "measurements",
      "title": "Measurements",
      "columns": 4,
      "collapsible": false,
      "collapsed": false,
      "fields": [
        {"key":"weightKg","label":"Weight (kg)","type":"number","required":false,"colSpan":1,"placeholder":"kg",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''29463-7'').valueQuantity.value","type":"quantity","loincCode":"29463-7","unit":"kg"},
         "showInTable":true},
        {"key":"heightCm","label":"Height (cm)","type":"number","required":false,"colSpan":1,"placeholder":"cm",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8302-2'').valueQuantity.value","type":"quantity","loincCode":"8302-2","unit":"cm"}},
        {"key":"bmi","label":"BMI","type":"computed","required":false,"colSpan":1,
         "computeExpression":"round(weightKg / ((heightCm/100) * (heightCm/100)), 1)",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''39156-5'').valueQuantity.value","type":"quantity","loincCode":"39156-5","unit":"kg/m2"},
         "showInTable":true},
        {"key":"bpSystolic","label":"BP Systolic","type":"number","required":false,"colSpan":1,"placeholder":"mmHg",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8480-6'').valueQuantity.value","type":"quantity","loincCode":"8480-6","unit":"mmHg"},
         "showInTable":true},
        {"key":"bpDiastolic","label":"BP Diastolic","type":"number","required":false,"colSpan":1,"placeholder":"mmHg",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8462-4'').valueQuantity.value","type":"quantity","loincCode":"8462-4","unit":"mmHg"},
         "showInTable":true},
        {"key":"pulse","label":"Pulse","type":"number","required":false,"colSpan":1,"placeholder":"/min",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8867-4'').valueQuantity.value","type":"quantity","loincCode":"8867-4","unit":"/min"},
         "showInTable":true},
        {"key":"respiration","label":"Respiration","type":"number","required":false,"colSpan":1,"placeholder":"/min",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''9279-1'').valueQuantity.value","type":"quantity","loincCode":"9279-1","unit":"/min"}},
        {"key":"temperatureC","label":"Temperature (°C)","type":"number","required":false,"colSpan":1,"placeholder":"°C",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''8310-5'').valueQuantity.value","type":"quantity","loincCode":"8310-5","unit":"Cel"},
         "showInTable":true},
        {"key":"oxygenSaturation","label":"O2 Saturation (%)","type":"number","required":false,"colSpan":1,"placeholder":"%",
         "fhirMapping":{"resource":"Observation","path":"component.where(code.coding.code=''2708-6'').valueQuantity.value","type":"quantity","loincCode":"2708-6","unit":"%"},
         "showInTable":true}
      ]
    },
    {
      "key": "vitals-meta",
      "title": "Recording Info",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"recordedAt","label":"Date/Time","type":"datetime","required":true,"colSpan":1,
         "fhirMapping":{"resource":"Observation","path":"effectiveDateTime","type":"datetime"},
         "showInTable":true},
        {"key":"signed","label":"E-Signed","type":"boolean","required":false,"colSpan":1,
         "fhirMapping":{"resource":"Observation","path":"status","type":"code"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":1,
         "fhirMapping":{"resource":"Observation","path":"note[0].text","type":"string"}}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'vitals' AND practice_type_code = '*' AND org_id = '*';

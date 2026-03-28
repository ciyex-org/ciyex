-- V169: Update appointment status labels and order
-- New status flow: Scheduled → Confirmed → Checked-in → Completed
-- Additional: Re-Scheduled, No Show, Cancelled
-- Uses valid FHIR codes as values with custom display labels.

UPDATE tab_field_config SET
  field_config = '{
  "sections": [
    {
      "key": "details",
      "title": "Appointment Details",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {"key":"appointmentType","label":"Visit Type","type":"select","required":true,"colSpan":1,
         "options":["Consultation","Follow-up","New Patient","Urgent","Routine","Annual Physical","Telehealth","Lab Work","Procedure","Referral"],
         "showInTable":true,
         "fhirMapping":{"resource":"Appointment","path":"appointmentType.text","type":"string"}},
        {"key":"status","label":"Status","type":"select","required":true,"colSpan":1,
         "options":[
           {"value":"booked",      "label":"Scheduled",     "color":"#3b82f6","triggersEncounter":false,"terminal":false,"order":1,"nextStatus":"pending"},
           {"value":"pending",     "label":"Confirmed",     "color":"#eab308","triggersEncounter":false,"terminal":false,"order":2,"nextStatus":"checked-in"},
           {"value":"checked-in",  "label":"Checked-in",    "color":"#10b981","triggersEncounter":true, "terminal":false,"order":3,"nextStatus":"fulfilled"},
           {"value":"fulfilled",   "label":"Completed",     "color":"#8b5cf6","triggersEncounter":false,"terminal":true, "order":4},
           {"value":"proposed",    "label":"Re-Scheduled",  "color":"#9ca3af","triggersEncounter":false,"terminal":false,"order":5},
           {"value":"noshow",      "label":"No Show",       "color":"#f97316","triggersEncounter":true, "terminal":true, "order":6,"encounterNote":"No-show"},
           {"value":"cancelled",   "label":"Cancelled",     "color":"#ef4444","triggersEncounter":false,"terminal":true, "order":7}
         ],
         "fhirMapping":{"resource":"Appointment","path":"status","type":"code"},
         "showInTable":true},
        {"key":"priority","label":"Priority","type":"select","required":false,"colSpan":1,
         "options":["routine","urgent","asap","stat"],
         "fhirMapping":{"resource":"Appointment","path":"priority","type":"unsignedInt"}},
        {"key":"start","label":"Start","type":"datetime","required":true,"colSpan":1,"showInTable":true,
         "fhirMapping":{"resource":"Appointment","path":"start","type":"instant"}},
        {"key":"end","label":"End","type":"datetime","required":true,"colSpan":1,"showInTable":true,
         "fhirMapping":{"resource":"Appointment","path":"end","type":"instant"}},
        {"key":"minutesDuration","label":"Duration (min)","type":"number","required":false,"colSpan":1,
         "fhirMapping":{"resource":"Appointment","path":"minutesDuration","type":"positiveInt"}},
        {"key":"reason","label":"Reason","type":"text","required":false,"colSpan":2,
         "fhirMapping":{"resource":"Appointment","path":"reasonCode[0].text","type":"string"}},
        {"key":"description","label":"Notes","type":"textarea","required":false,"colSpan":1,
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
         "fhirMapping":{"resource":"Appointment","path":"participant.where(type.coding.code=''patient'').actor","type":"reference"}},
        {"key":"provider","label":"Provider","type":"lookup","required":true,"colSpan":1,
         "lookupConfig":{"endpoint":"/api/providers","displayField":"name","valueField":"fhirId","searchable":true},
         "fhirMapping":{"resource":"Appointment","path":"participant.where(type.coding.code=''practitioner'').actor","type":"reference"},
         "showInTable":true},
        {"key":"location","label":"Location","type":"lookup","required":true,"colSpan":1,
         "lookupConfig":{"endpoint":"/api/locations","displayField":"name","valueField":"fhirId","searchable":true},
         "fhirMapping":{"resource":"Appointment","path":"participant.where(type.coding.code=''location'').actor","type":"reference"},
         "showInTable":true}
      ]
    }
  ]
}',
  version = version + 1,
  updated_at = now()
WHERE tab_key = 'appointments' AND practice_type_code = '*' AND org_id = '*';

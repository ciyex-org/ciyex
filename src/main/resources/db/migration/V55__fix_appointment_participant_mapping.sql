-- V55: Fix appointment participant mapping
-- Index-based paths (participant[0], [1], [2]) break after FHIR server round-trip
-- because HAPI FHIR does not guarantee participant array order.
-- Switch to where()-filter paths that identify participants by type code,
-- which survives reordering.

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
         "options":["proposed","pending","booked","arrived","fulfilled","cancelled","noshow","entered-in-error"],
         "fhirMapping":{"resource":"Appointment","path":"status","type":"code"}},
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

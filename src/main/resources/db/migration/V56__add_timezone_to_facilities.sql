-- V56: Add timezone field to facilities (Location resources)
-- Allows each facility/location to have its own IANA timezone.
-- Used as primary timezone for provider schedules at that location,
-- with practice-level timezone as fallback.

UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "facility-info",
      "title": "Facility Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "name",
          "label": "Facility Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Facility name",
          "fhirMapping": {"resource": "Location", "path": "name", "type": "string"},
          "showInTable": true
        },
        {
          "key": "status",
          "label": "Status",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "active", "label": "Active"},
            {"value": "suspended", "label": "Suspended"},
            {"value": "inactive", "label": "Inactive"}
          ],
          "fhirMapping": {"resource": "Location", "path": "status", "type": "string"},
          "showInTable": true
        },
        {
          "key": "description",
          "label": "Description",
          "type": "text",
          "required": false,
          "colSpan": 2,
          "placeholder": "Facility description",
          "fhirMapping": {"resource": "Location", "path": "description", "type": "string"}
        },
        {
          "key": "timezone",
          "label": "Time Zone",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "placeholder": "Select time zone",
          "options": [
            {"value": "America/New_York", "label": "Eastern (ET)"},
            {"value": "America/Chicago", "label": "Central (CT)"},
            {"value": "America/Denver", "label": "Mountain (MT)"},
            {"value": "America/Los_Angeles", "label": "Pacific (PT)"},
            {"value": "America/Anchorage", "label": "Alaska (AKT)"},
            {"value": "Pacific/Honolulu", "label": "Hawaii (HT)"},
            {"value": "America/Phoenix", "label": "Arizona (no DST)"},
            {"value": "America/Puerto_Rico", "label": "Atlantic (AT)"}
          ],
          "fhirMapping": {"resource": "Location", "path": "extension[url=http://ciyex.com/fhir/StructureDefinition/timezone].valueString", "type": "string"},
          "showInTable": true
        },
        {
          "key": "phone",
          "label": "Phone",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "Location", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "fax",
          "label": "Fax",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4568",
          "fhirMapping": {"resource": "Location", "path": "telecom.where(system=''fax'').value", "type": "string"}
        },
        {
          "key": "email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "facility@practice.com",
          "fhirMapping": {"resource": "Location", "path": "telecom.where(system=''email'').value", "type": "string"}
        }
      ]
    },
    {
      "key": "address",
      "title": "Address",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "address.line1",
          "label": "Address Line 1",
          "type": "text",
          "required": false,
          "colSpan": 2,
          "placeholder": "Street address",
          "fhirMapping": {"resource": "Location", "path": "address.line[0]", "type": "string"}
        },
        {
          "key": "address.line2",
          "label": "Address Line 2",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Location", "path": "address.line[1]", "type": "string"}
        },
        {
          "key": "address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Location", "path": "address.city", "type": "string"}
        },
        {
          "key": "address.state",
          "label": "State",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Location", "path": "address.state", "type": "string"}
        },
        {
          "key": "address.zip",
          "label": "ZIP Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Location", "path": "address.postalCode", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'facilities' AND org_id = '*';

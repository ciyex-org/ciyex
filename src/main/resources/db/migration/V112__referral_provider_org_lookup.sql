-- Update referral-providers: change organization field from text to lookup with autoFill
-- When a referral practice is selected, contact info and address auto-populate
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "personal-info",
      "title": "Provider Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "identification.prefix",
          "label": "Prefix",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": [
            {"value": "Dr.", "label": "Dr."},
            {"value": "Mr.", "label": "Mr."},
            {"value": "Mrs.", "label": "Mrs."},
            {"value": "Ms.", "label": "Ms."}
          ],
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].prefix[0]", "type": "string"}
        },
        {
          "key": "identification.firstName",
          "label": "First Name",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "First name",
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].given[0]", "type": "string"},
          "showInTable": true
        },
        {
          "key": "identification.lastName",
          "label": "Last Name",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "Last name",
          "fhirMapping": {"resource": "Practitioner", "path": "name[0].family", "type": "string"},
          "showInTable": true
        },
        {
          "key": "specialty",
          "label": "Specialty",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. Cardiology",
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].code.text", "type": "string"},
          "showInTable": true
        },
        {
          "key": "npi",
          "label": "NPI",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "10-digit NPI",
          "fhirMapping": {"resource": "Practitioner", "path": "identifier.where(system=''http://hl7.org/fhir/sid/us-npi'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "organization",
          "label": "Organization",
          "type": "lookup",
          "required": false,
          "colSpan": 1,
          "placeholder": "Search referral practices...",
          "lookupConfig": {
            "endpoint": "/api/fhir-resource/referral-practices",
            "displayField": "name",
            "valueField": "name",
            "searchable": true
          },
          "autoFill": {
            "phone": "phone",
            "fax": "fax",
            "email": "email",
            "address.line1": "address.line1",
            "address.city": "address.city",
            "address.state": "address.state",
            "address.zip": "address.zip"
          },
          "fhirMapping": {"resource": "Practitioner", "path": "qualification[0].issuer.display", "type": "string"},
          "showInTable": true
        }
      ]
    },
    {
      "key": "contact",
      "title": "Contact Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "phone",
          "label": "Phone",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "fax",
          "label": "Fax",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4568",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom.where(system=''fax'').value", "type": "string"}
        },
        {
          "key": "email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "doctor@example.com",
          "fhirMapping": {"resource": "Practitioner", "path": "telecom.where(system=''email'').value", "type": "string"}
        }
      ]
    },
    {
      "key": "address",
      "title": "Address",
      "columns": 3,
      "collapsible": true,
      "collapsed": true,
      "fields": [
        {
          "key": "address.line1",
          "label": "Address Line 1",
          "type": "text",
          "required": false,
          "colSpan": 2,
          "placeholder": "Street address",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].line[0]", "type": "string"}
        },
        {
          "key": "address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "City",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].city", "type": "string"}
        },
        {
          "key": "address.state",
          "label": "State",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "State",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].state", "type": "string"}
        },
        {
          "key": "address.zip",
          "label": "ZIP Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "ZIP code",
          "fhirMapping": {"resource": "Practitioner", "path": "address[0].postalCode", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'referral-providers' AND org_id = '*';

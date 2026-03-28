-- V33: Field configurations for Settings pages (Practice, Insurance, Facilities, Services, Referral)

-- ==================== PRACTICE ====================
UPDATE tab_field_config
SET field_config = '{
  "singleton": true,
  "sections": [
    {
      "key": "practice-info",
      "title": "Practice Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "name",
          "label": "Practice Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Practice name",
          "fhirMapping": {"resource": "Organization", "path": "name", "type": "string"},
          "showInTable": true
        },
        {
          "key": "active",
          "label": "Active",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "active", "type": "boolean"},
          "showInTable": true
        },
        {
          "key": "npi",
          "label": "NPI",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "10-digit NPI",
          "fhirMapping": {"resource": "Organization", "path": "identifier.where(system=''http://hl7.org/fhir/sid/us-npi'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "taxId",
          "label": "Tax ID (EIN)",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "XX-XXXXXXX",
          "fhirMapping": {"resource": "Organization", "path": "identifier.where(system=''urn:oid:2.16.840.1.113883.4.4'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "alias",
          "label": "DBA / Alias",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Doing business as",
          "fhirMapping": {"resource": "Organization", "path": "alias[0]", "type": "string"}
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
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "fax",
          "label": "Fax",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4568",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''fax'').value", "type": "string"}
        },
        {
          "key": "email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "office@practice.com",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''email'').value", "type": "string"}
        },
        {
          "key": "website",
          "label": "Website",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "https://www.practice.com",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''url'').value", "type": "string"}
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
          "fhirMapping": {"resource": "Organization", "path": "address[0].line[0]", "type": "string"}
        },
        {
          "key": "address.line2",
          "label": "Address Line 2",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Suite, unit, etc.",
          "fhirMapping": {"resource": "Organization", "path": "address[0].line[1]", "type": "string"}
        },
        {
          "key": "address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "City",
          "fhirMapping": {"resource": "Organization", "path": "address[0].city", "type": "string"}
        },
        {
          "key": "address.state",
          "label": "State",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "State",
          "fhirMapping": {"resource": "Organization", "path": "address[0].state", "type": "string"}
        },
        {
          "key": "address.zip",
          "label": "ZIP Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "ZIP code",
          "fhirMapping": {"resource": "Organization", "path": "address[0].postalCode", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'practice' AND org_id = '*';

-- ==================== INSURANCE ====================
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "company-info",
      "title": "Insurance Company",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "name",
          "label": "Company Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Insurance company name",
          "fhirMapping": {"resource": "Organization", "path": "name", "type": "string"},
          "showInTable": true
        },
        {
          "key": "active",
          "label": "Active",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "active", "type": "boolean"},
          "showInTable": true
        },
        {
          "key": "payerId",
          "label": "Payer ID",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "EDI Payer ID",
          "fhirMapping": {"resource": "Organization", "path": "identifier.where(system=''urn:oid:2.16.840.1.113883.4.6'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "phone",
          "label": "Phone",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "fax",
          "label": "Fax",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4568",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''fax'').value", "type": "string"}
        },
        {
          "key": "email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "claims@insurance.com",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''email'').value", "type": "string"}
        },
        {
          "key": "website",
          "label": "Website",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "https://provider.insurance.com",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''url'').value", "type": "string"}
        }
      ]
    },
    {
      "key": "address",
      "title": "Claims Address",
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
          "fhirMapping": {"resource": "Organization", "path": "address[0].line[0]", "type": "string"}
        },
        {
          "key": "address.line2",
          "label": "Address Line 2",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Suite, PO Box",
          "fhirMapping": {"resource": "Organization", "path": "address[0].line[1]", "type": "string"}
        },
        {
          "key": "address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].city", "type": "string"}
        },
        {
          "key": "address.state",
          "label": "State",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].state", "type": "string"}
        },
        {
          "key": "address.zip",
          "label": "ZIP Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].postalCode", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'insurance' AND org_id = '*';

-- ==================== FACILITIES ====================
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
          "colSpan": 3,
          "placeholder": "Facility description",
          "fhirMapping": {"resource": "Location", "path": "description", "type": "string"}
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

-- ==================== SERVICES ====================
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "service-info",
      "title": "Service Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "name",
          "label": "Service Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Service name",
          "fhirMapping": {"resource": "HealthcareService", "path": "name", "type": "string"},
          "showInTable": true
        },
        {
          "key": "active",
          "label": "Active",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "HealthcareService", "path": "active", "type": "boolean"},
          "showInTable": true
        },
        {
          "key": "comment",
          "label": "Description",
          "type": "textarea",
          "required": false,
          "colSpan": 3,
          "placeholder": "Service description",
          "fhirMapping": {"resource": "HealthcareService", "path": "comment", "type": "string"}
        },
        {
          "key": "phone",
          "label": "Phone",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "HealthcareService", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "appointmentRequired",
          "label": "Appointment Required",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "HealthcareService", "path": "appointmentRequired", "type": "boolean"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'services' AND org_id = '*';

-- ==================== REFERRAL PRACTICES ====================
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "referral-info",
      "title": "Referral Practice",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "name",
          "label": "Practice Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Referral practice name",
          "fhirMapping": {"resource": "Organization", "path": "name", "type": "string"},
          "showInTable": true
        },
        {
          "key": "active",
          "label": "Active",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "active", "type": "boolean"},
          "showInTable": true
        },
        {
          "key": "npi",
          "label": "NPI",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Organization NPI",
          "fhirMapping": {"resource": "Organization", "path": "identifier.where(system=''http://hl7.org/fhir/sid/us-npi'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "phone",
          "label": "Phone",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4567",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''phone'').value", "type": "string"},
          "showInTable": true
        },
        {
          "key": "fax",
          "label": "Fax",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "(555) 123-4568",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''fax'').value", "type": "string"}
        },
        {
          "key": "email",
          "label": "Email",
          "type": "email",
          "required": false,
          "colSpan": 1,
          "placeholder": "referral@practice.com",
          "fhirMapping": {"resource": "Organization", "path": "telecom.where(system=''email'').value", "type": "string"}
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
          "fhirMapping": {"resource": "Organization", "path": "address[0].line[0]", "type": "string"}
        },
        {
          "key": "address.city",
          "label": "City",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].city", "type": "string"}
        },
        {
          "key": "address.state",
          "label": "State",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].state", "type": "string"}
        },
        {
          "key": "address.zip",
          "label": "ZIP Code",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "Organization", "path": "address[0].postalCode", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'referral-practices' AND org_id = '*';

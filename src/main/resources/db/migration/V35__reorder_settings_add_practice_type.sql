-- Reorder settings sidebar items
UPDATE tab_field_config SET position = 0  WHERE tab_key = 'practice'            AND org_id = '*';
UPDATE tab_field_config SET position = 1  WHERE tab_key = 'facilities'          AND org_id = '*';
UPDATE tab_field_config SET position = 2  WHERE tab_key = 'providers'           AND org_id = '*';
UPDATE tab_field_config SET position = 3  WHERE tab_key = 'insurance'           AND org_id = '*';
UPDATE tab_field_config SET position = 4  WHERE tab_key = 'referral-practices'  AND org_id = '*';
UPDATE tab_field_config SET position = 5  WHERE tab_key = 'referral-providers'  AND org_id = '*';
UPDATE tab_field_config SET position = 6  WHERE tab_key = 'codes'              AND org_id = '*';
UPDATE tab_field_config SET position = 7  WHERE tab_key = 'services'           AND org_id = '*';
UPDATE tab_field_config SET position = 8  WHERE tab_key = 'template-documents' AND org_id = '*';

-- Add Practice Type field to Practice field config
-- Uses Organization.type[0].text to store the human-readable practice type
-- (type[0].coding[0].code already holds "prov" for the category)
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
          "key": "practiceType",
          "label": "Practice Type",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "placeholder": "Select practice type",
          "options": [
            {"value": "general", "label": "General / Family Medicine"},
            {"value": "internal", "label": "Internal Medicine"},
            {"value": "pediatrics", "label": "Pediatrics"},
            {"value": "dental", "label": "Dental"},
            {"value": "cardiology", "label": "Cardiology"},
            {"value": "orthopedics", "label": "Orthopedics"},
            {"value": "dermatology", "label": "Dermatology"},
            {"value": "ophthalmology", "label": "Ophthalmology"},
            {"value": "psychiatry", "label": "Psychiatry"},
            {"value": "obgyn", "label": "OB/GYN"},
            {"value": "urgent-care", "label": "Urgent Care"},
            {"value": "multi-specialty", "label": "Multi-Specialty"},
            {"value": "other", "label": "Other"}
          ],
          "fhirMapping": {"resource": "Organization", "path": "type[0].text", "type": "string"},
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

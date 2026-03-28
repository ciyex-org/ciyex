-- Field config for Referral Providers (external Practitioner resources)
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
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "Practice / Organization name",
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

-- Field config for Template Documents (DocumentReference resources)
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "document-info",
      "title": "Document Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "description",
          "label": "Title",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Document title",
          "fhirMapping": {"resource": "DocumentReference", "path": "description", "type": "string"},
          "showInTable": true
        },
        {
          "key": "status",
          "label": "Status",
          "type": "select",
          "required": true,
          "colSpan": 1,
          "options": [
            {"value": "current", "label": "Current"},
            {"value": "superseded", "label": "Superseded"},
            {"value": "entered-in-error", "label": "Entered in Error"}
          ],
          "fhirMapping": {"resource": "DocumentReference", "path": "status", "type": "code"},
          "showInTable": true
        },
        {
          "key": "category",
          "label": "Category",
          "type": "combobox",
          "required": false,
          "colSpan": 1,
          "placeholder": "Select or type category",
          "options": [
            {"value": "consent", "label": "Consent Form"},
            {"value": "intake", "label": "Intake Form"},
            {"value": "referral", "label": "Referral Letter"},
            {"value": "lab-order", "label": "Lab Order"},
            {"value": "prescription", "label": "Prescription"},
            {"value": "clinical-note", "label": "Clinical Note"},
            {"value": "discharge", "label": "Discharge Summary"},
            {"value": "imaging", "label": "Imaging Report"},
            {"value": "insurance", "label": "Insurance Document"},
            {"value": "other", "label": "Other"}
          ],
          "fhirMapping": {"resource": "DocumentReference", "path": "type.text", "type": "string"},
          "showInTable": true
        },
        {
          "key": "date",
          "label": "Date",
          "type": "date",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "DocumentReference", "path": "date", "type": "datetime"},
          "showInTable": true
        }
      ]
    }
  ]
}'
WHERE tab_key = 'template-documents' AND org_id = '*';

-- Add showInTable to key fields in providers config
-- (Update the JSON to add showInTable: true to firstName, lastName, specialty, NPI, phone, status)
UPDATE tab_field_config
SET field_config = jsonb_set(field_config::jsonb,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'personal-info' THEN
          jsonb_set(section, '{fields}',
            (SELECT jsonb_agg(
              CASE
                WHEN f->>'key' IN ('identification.firstName', 'identification.lastName', 'npi')
                THEN f || '{"showInTable": true}'::jsonb
                ELSE f
              END
            ) FROM jsonb_array_elements(section->'fields') f)
          )
        WHEN section->>'key' = 'professional-details' THEN
          jsonb_set(section, '{fields}',
            (SELECT jsonb_agg(
              CASE
                WHEN f->>'key' IN ('professionalDetails.specialty', 'professionalDetails.providerType')
                THEN f || '{"showInTable": true}'::jsonb
                ELSE f
              END
            ) FROM jsonb_array_elements(section->'fields') f)
          )
        WHEN section->>'key' = 'contact-info' THEN
          jsonb_set(section, '{fields}',
            (SELECT jsonb_agg(
              CASE
                WHEN f->>'key' IN ('contact.phoneNumber', 'contact.email')
                THEN f || '{"showInTable": true}'::jsonb
                ELSE f
              END
            ) FROM jsonb_array_elements(section->'fields') f)
          )
        WHEN section->>'key' = 'system-access' THEN
          jsonb_set(section, '{fields}',
            (SELECT jsonb_agg(
              CASE
                WHEN f->>'key' = 'systemAccess.status'
                THEN f || '{"showInTable": true}'::jsonb
                ELSE f
              END
            ) FROM jsonb_array_elements(section->'fields') f)
          )
        ELSE section
      END
    ) FROM jsonb_array_elements(field_config::jsonb->'sections') section
  )
)
WHERE tab_key = 'providers' AND org_id = '*';

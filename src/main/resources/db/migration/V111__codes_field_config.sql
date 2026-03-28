-- Populate field config for the Codes settings page (was empty '{}' from V25)
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "code-info",
      "title": "Code Information",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "codeType",
          "label": "Code Type",
          "type": "select",
          "required": true,
          "colSpan": 1,
          "options": [
            {"value": "ICD10", "label": "ICD-10"},
            {"value": "ICD9", "label": "ICD-9"},
            {"value": "CPT4", "label": "CPT-4"},
            {"value": "HCPCS", "label": "HCPCS"},
            {"value": "CUSTOM", "label": "Custom"}
          ],
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''codeType'').valueString", "type": "string"},
          "showInTable": true
        },
        {
          "key": "code",
          "label": "Code",
          "type": "text",
          "required": true,
          "colSpan": 1,
          "placeholder": "e.g. I10, 99214",
          "fhirMapping": {"resource": "CodeSystem", "path": "concept[0].code", "type": "string"},
          "showInTable": true
        },
        {
          "key": "modifier",
          "label": "Modifier",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. 25, 59",
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''modifier'').valueString", "type": "string"},
          "showInTable": true
        },
        {
          "key": "shortDescription",
          "label": "Short Description",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Brief label",
          "fhirMapping": {"resource": "CodeSystem", "path": "concept[0].display", "type": "string"},
          "showInTable": true
        },
        {
          "key": "category",
          "label": "Category",
          "type": "combobox",
          "required": true,
          "colSpan": 1,
          "placeholder": "Select or type category",
          "options": [
            {"value": "E&M", "label": "E&M"},
            {"value": "Procedure", "label": "Procedure"},
            {"value": "Lab", "label": "Lab"},
            {"value": "Radiology", "label": "Radiology"},
            {"value": "Diagnosis", "label": "Diagnosis"},
            {"value": "Supply", "label": "Supply"},
            {"value": "Other", "label": "Other"}
          ],
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''category'').valueString", "type": "string"},
          "showInTable": true
        },
        {
          "key": "description",
          "label": "Description",
          "type": "textarea",
          "required": false,
          "colSpan": 3,
          "placeholder": "Full description",
          "fhirMapping": {"resource": "CodeSystem", "path": "concept[0].definition", "type": "string"}
        }
      ]
    },
    {
      "key": "billing-info",
      "title": "Billing & Reporting",
      "columns": 3,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "feeStandard",
          "label": "Standard Fee",
          "type": "number",
          "required": true,
          "colSpan": 1,
          "placeholder": "0.00",
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''feeStandard'').valueDecimal", "type": "string"},
          "showInTable": true
        },
        {
          "key": "diagnosisReporting",
          "label": "Diagnosis Reporting",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''diagnosisReporting'').valueBoolean", "type": "boolean"}
        },
        {
          "key": "serviceReporting",
          "label": "Service Reporting",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''serviceReporting'').valueBoolean", "type": "boolean"}
        },
        {
          "key": "active",
          "label": "Active",
          "type": "toggle",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "CodeSystem", "path": "status", "type": "code"},
          "showInTable": true
        },
        {
          "key": "relateTo",
          "label": "Related To",
          "type": "text",
          "required": false,
          "colSpan": 2,
          "placeholder": "Related code or group",
          "fhirMapping": {"resource": "CodeSystem", "path": "property.where(code=''relateTo'').valueString", "type": "string"}
        }
      ]
    }
  ]
}'
WHERE tab_key = 'codes' AND org_id = '*';

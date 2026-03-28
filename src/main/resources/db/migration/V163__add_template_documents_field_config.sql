-- V163: Add field configuration for Template Documents settings tab (DocumentReference)
-- Previously the field_config was '{}' causing "No field configuration found" in the UI form.

UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "document-info",
      "title": "Document Information",
      "columns": 2,
      "collapsible": false,
      "fields": [
        {
          "key": "description",
          "label": "Document Name",
          "type": "text",
          "required": true,
          "colSpan": 2,
          "placeholder": "Template document name",
          "fhirMapping": {"resource": "DocumentReference", "path": "description", "type": "string"},
          "showInTable": true
        },
        {
          "key": "status",
          "label": "Status",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": ["current", "superseded", "entered-in-error"],
          "fhirMapping": {"resource": "DocumentReference", "path": "status", "type": "code"},
          "showInTable": true
        },
        {
          "key": "docStatus",
          "label": "Document Status",
          "type": "select",
          "required": false,
          "colSpan": 1,
          "options": ["preliminary", "final", "amended", "entered-in-error"],
          "fhirMapping": {"resource": "DocumentReference", "path": "docStatus", "type": "code"},
          "showInTable": false
        },
        {
          "key": "category",
          "label": "Category",
          "type": "text",
          "required": false,
          "colSpan": 1,
          "placeholder": "e.g. Clinical Note, Consent Form",
          "showInTable": true
        },
        {
          "key": "date",
          "label": "Date",
          "type": "date",
          "required": false,
          "colSpan": 1,
          "fhirMapping": {"resource": "DocumentReference", "path": "date", "type": "datetime"},
          "showInTable": false
        }
      ]
    },
    {
      "key": "document-content",
      "title": "Content",
      "columns": 1,
      "collapsible": true,
      "collapsed": false,
      "fields": [
        {
          "key": "content",
          "label": "Template Content",
          "type": "textarea",
          "required": false,
          "colSpan": 1,
          "placeholder": "Enter template text or leave blank"
        }
      ]
    }
  ]
}'::jsonb
WHERE tab_key = 'template-documents'
  AND org_id = '*';

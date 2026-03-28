-- Add field configuration for Services settings tab (HealthcareService)
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "title": "Service Information",
      "fields": [
        { "key": "name", "label": "Service Name", "type": "text", "required": true, "showInTable": true },
        { "key": "type", "label": "Service Type", "type": "text", "showInTable": true },
        { "key": "category", "label": "Category", "type": "text", "showInTable": true },
        { "key": "description", "label": "Description", "type": "textarea" },
        { "key": "specialty", "label": "Specialty", "type": "text", "showInTable": true },
        { "key": "active", "label": "Active", "type": "boolean", "showInTable": true }
      ]
    },
    {
      "title": "Details",
      "fields": [
        { "key": "duration", "label": "Duration (minutes)", "type": "number" },
        { "key": "fee", "label": "Fee", "type": "number" },
        { "key": "notes", "label": "Notes", "type": "textarea" }
      ]
    }
  ]
}'
WHERE tab_key = 'services';

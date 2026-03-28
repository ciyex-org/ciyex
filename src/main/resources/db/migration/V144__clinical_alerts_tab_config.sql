-- Add Clinical Alerts as a patient chart tab (uses FHIR Flag resource)
INSERT INTO tab_field_config (
    tab_key, practice_type_code, org_id, label, icon, category, category_position, position, visible,
    fhir_resources, field_config
) VALUES (
    'clinical-alerts', '*', '*', 'Clinical Alerts', 'AlertTriangle', 'CLINICAL', 1, 15, true,
    '[{"type": "Flag", "patientSearchParam": "patient"}]',
    '{
      "sections": [
        {
          "key": "alert-info",
          "title": "Clinical Alert",
          "columns": 2,
          "collapsible": false,
          "collapsed": false,
          "fields": [
            {"key": "alertName", "label": "Alert", "type": "text", "required": true, "colSpan": 1,
             "fhirMapping": {"resource": "Flag", "path": "code.text", "type": "string"},
             "showInTable": true},
            {"key": "status", "label": "Status", "type": "select", "required": true, "colSpan": 1,
             "options": [{"value": "active", "label": "Active"}, {"value": "inactive", "label": "Inactive"}, {"value": "entered-in-error", "label": "Entered in Error"}],
             "fhirMapping": {"resource": "Flag", "path": "status", "type": "code"},
             "showInTable": true},
            {"key": "category", "label": "Category", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "clinical", "label": "Clinical"}, {"value": "safety", "label": "Safety"}, {"value": "behavioral", "label": "Behavioral"}, {"value": "drug", "label": "Drug Interaction"}, {"value": "allergy", "label": "Allergy"}, {"value": "other", "label": "Other"}],
             "fhirMapping": {"resource": "Flag", "path": "category[0].coding[0].code", "type": "code"},
             "showInTable": true},
            {"key": "severity", "label": "Severity", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "high", "label": "High"}, {"value": "medium", "label": "Medium"}, {"value": "low", "label": "Low"}],
             "fhirMapping": {"resource": "Flag", "path": "extension[url=http://ciyex.org/fhir/ext/alert-severity].valueCode", "type": "code"},
             "showInTable": true},
            {"key": "identifiedDate", "label": "Identified Date", "type": "date", "required": false, "colSpan": 1, "defaultToday": true,
             "fhirMapping": {"resource": "Flag", "path": "period.start", "type": "date"},
             "showInTable": true},
            {"key": "endDate", "label": "End Date", "type": "date", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Flag", "path": "period.end", "type": "date"}},
            {"key": "author", "label": "Author", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Flag", "path": "author.display", "type": "string"},
             "showInTable": true},
            {"key": "notes", "label": "Notes", "type": "textarea", "required": false, "colSpan": 2,
             "fhirMapping": {"resource": "Flag", "path": "extension[url=http://ciyex.org/fhir/ext/alert-notes].valueString", "type": "string"}}
          ]
        }
      ]
    }'
)
ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET fhir_resources = EXCLUDED.fhir_resources,
    field_config = EXCLUDED.field_config,
    label = EXCLUDED.label,
    icon = EXCLUDED.icon,
    category = EXCLUDED.category,
    category_position = EXCLUDED.category_position,
    position = EXCLUDED.position,
    visible = EXCLUDED.visible;

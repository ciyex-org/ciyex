-- Add Insurance Coverage as a patient chart tab (uses FHIR Coverage resource)
INSERT INTO tab_field_config (
    tab_key, practice_type_code, org_id, label, icon, category, category_position, position, visible,
    fhir_resources, field_config
) VALUES (
    'insurance-coverage', '*', '*', 'Insurance', 'Shield', 'GENERAL', 3, 0, true,
    '[{"type": "Coverage", "searchParam": "beneficiary=Patient/{patientId}"}]',
    '{
      "sections": [
        {
          "key": "policy-info",
          "title": "Policy Information",
          "columns": 3,
          "collapsible": true,
          "collapsed": false,
          "fields": [
            {"key": "insuranceType", "label": "Insurance Tier", "type": "select", "required": true, "colSpan": 1,
             "options": [{"value": "primary", "label": "Primary"}, {"value": "secondary", "label": "Secondary"}, {"value": "tertiary", "label": "Tertiary"}],
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/insurance-tier].valueCode", "type": "code"},
             "showInTable": true},
            {"key": "payerName", "label": "Insurance Company / Payer", "type": "text", "required": true, "colSpan": 1, "placeholder": "Insurance company name",
             "fhirMapping": {"resource": "Coverage", "path": "payor[0].display", "type": "string"},
             "showInTable": true},
            {"key": "planName", "label": "Plan Name", "type": "text", "required": false, "colSpan": 1, "placeholder": "e.g. Blue Cross PPO Gold",
             "fhirMapping": {"resource": "Coverage", "path": "class[0].value", "type": "string"},
             "showInTable": true},
            {"key": "policyNumber", "label": "Policy / Member ID", "type": "text", "required": true, "colSpan": 1, "placeholder": "Member ID",
             "fhirMapping": {"resource": "Coverage", "path": "identifier[0].value", "type": "string"},
             "showInTable": true},
            {"key": "groupNumber", "label": "Group Number", "type": "text", "required": false, "colSpan": 1, "placeholder": "Group #",
             "fhirMapping": {"resource": "Coverage", "path": "class[1].value", "type": "string"},
             "showInTable": true},
            {"key": "policyType", "label": "Policy Type", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "HMO", "label": "HMO"}, {"value": "PPO", "label": "PPO"}, {"value": "EPO", "label": "EPO"}, {"value": "POS", "label": "POS"}, {"value": "HDHP", "label": "HDHP"}, {"value": "Medicare", "label": "Medicare"}, {"value": "Medicaid", "label": "Medicaid"}, {"value": "Tricare", "label": "TRICARE"}, {"value": "Workers-Comp", "label": "Workers'' Comp"}, {"value": "Other", "label": "Other"}],
             "fhirMapping": {"resource": "Coverage", "path": "type.text", "type": "string"}},
            {"key": "copayAmount", "label": "Copay Amount", "type": "text", "required": false, "colSpan": 1, "placeholder": "$0.00",
             "fhirMapping": {"resource": "Coverage", "path": "costToBeneficiary[0].valueMoney.value", "type": "decimal"}},
            {"key": "policyEffectiveDate", "label": "Effective Date", "type": "date", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "period.start", "type": "date"}},
            {"key": "policyEndDate", "label": "End Date", "type": "date", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "period.end", "type": "date"}},
            {"key": "status", "label": "Status", "type": "select", "required": true, "colSpan": 1,
             "options": [{"value": "active", "label": "Active"}, {"value": "cancelled", "label": "Cancelled"}, {"value": "draft", "label": "Draft"}, {"value": "entered-in-error", "label": "Entered in Error"}],
             "fhirMapping": {"resource": "Coverage", "path": "status", "type": "code"},
             "showInTable": true}
          ]
        },
        {
          "key": "subscriber-info",
          "title": "Subscriber Information",
          "columns": 3,
          "collapsible": true,
          "collapsed": false,
          "fields": [
            {"key": "subscriberRelationship", "label": "Relationship to Patient", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "self", "label": "Self"}, {"value": "spouse", "label": "Spouse"}, {"value": "child", "label": "Child"}, {"value": "parent", "label": "Parent"}, {"value": "other", "label": "Other"}],
             "fhirMapping": {"resource": "Coverage", "path": "relationship.coding[0].code", "type": "code"}},
            {"key": "subscriberFirstName", "label": "Subscriber First Name", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-first-name].valueString", "type": "string"}},
            {"key": "subscriberLastName", "label": "Subscriber Last Name", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-last-name].valueString", "type": "string"}},
            {"key": "subscriberDOB", "label": "Subscriber Date of Birth", "type": "date", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-dob].valueDate", "type": "date"}},
            {"key": "subscriberGender", "label": "Subscriber Sex", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "male", "label": "Male"}, {"value": "female", "label": "Female"}, {"value": "other", "label": "Other"}],
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-gender].valueCode", "type": "code"}},
            {"key": "subscriberSSN", "label": "Subscriber SSN", "type": "text", "required": false, "colSpan": 1, "placeholder": "XXX-XX-XXXX",
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-ssn].valueString", "type": "string"}},
            {"key": "subscriberPhone", "label": "Subscriber Phone", "type": "phone", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-phone].valueString", "type": "string"}},
            {"key": "subscriberAddress", "label": "Subscriber Address", "type": "text", "required": false, "colSpan": 2, "placeholder": "Full address",
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-address].valueString", "type": "string"}},
            {"key": "subscriberEmployer", "label": "Subscriber Employer", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Coverage", "path": "extension[url=http://ciyex.org/fhir/ext/subscriber-employer].valueString", "type": "string"}}
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

-- V71: Portal-facing demographics tab_field_config
-- Uses the SAME FHIR Patient resource and fhirMapping paths as EHR demographics,
-- but exposes only patient-appropriate fields for self-service editing.
-- Fully configurable: admin can add/remove/reorder fields via tab_field_config without code changes.

INSERT INTO tab_field_config (
    tab_key, practice_type_code, org_id,
    fhir_resources, field_config,
    label, icon, category, position, visible
) VALUES (
    'portal-demographics', '*', '*',
    '[{"type": "Patient"}]',
    '{
      "sections": [
        {
          "key": "personal-info",
          "title": "Personal Information",
          "columns": 2,
          "collapsible": false,
          "fields": [
            {"key": "firstName", "label": "First Name", "type": "text", "required": true, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "name[0].given[0]", "type": "string"}},
            {"key": "lastName", "label": "Last Name", "type": "text", "required": true, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "name[0].family", "type": "string"}},
            {"key": "middleName", "label": "Middle Name", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "name[0].given[1]", "type": "string"}},
            {"key": "dateOfBirth", "label": "Date of Birth", "type": "date", "required": true, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "birthDate", "type": "date"}},
            {"key": "gender", "label": "Sex at Birth", "type": "select", "required": true, "colSpan": 1,
             "options": [{"value": "male", "label": "Male"}, {"value": "female", "label": "Female"}, {"value": "other", "label": "Other"}, {"value": "unknown", "label": "Unknown"}],
             "fhirMapping": {"resource": "Patient", "path": "gender", "type": "code"}},
            {"key": "maritalStatus", "label": "Marital Status", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "S", "label": "Single"}, {"value": "M", "label": "Married"}, {"value": "D", "label": "Divorced"}, {"value": "W", "label": "Widowed"}, {"value": "T", "label": "Domestic Partner"}, {"value": "L", "label": "Legally Separated"}],
             "fhirMapping": {"resource": "Patient", "path": "maritalStatus.coding[0].code", "type": "code"}},
            {"key": "language", "label": "Preferred Language", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "en", "label": "English"}, {"value": "es", "label": "Spanish"}, {"value": "zh", "label": "Chinese"}, {"value": "vi", "label": "Vietnamese"}, {"value": "ko", "label": "Korean"}, {"value": "tl", "label": "Tagalog"}, {"value": "fr", "label": "French"}, {"value": "ar", "label": "Arabic"}, {"value": "de", "label": "German"}, {"value": "ru", "label": "Russian"}, {"value": "pt", "label": "Portuguese"}, {"value": "hi", "label": "Hindi"}, {"value": "ja", "label": "Japanese"}, {"value": "other", "label": "Other"}],
             "fhirMapping": {"resource": "Patient", "path": "communication[0].language.coding[0].code", "type": "code"}},
            {"key": "race", "label": "Race", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "2106-3", "label": "White"}, {"value": "2054-5", "label": "Black or African American"}, {"value": "2028-9", "label": "Asian"}, {"value": "1002-5", "label": "American Indian or Alaska Native"}, {"value": "2076-8", "label": "Native Hawaiian or Pacific Islander"}, {"value": "2131-1", "label": "Other Race"}, {"value": "ASKU", "label": "Asked but unknown"}, {"value": "UNK", "label": "Unknown"}],
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-race].valueCode", "type": "code"}},
            {"key": "ethnicity", "label": "Ethnicity", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "2135-2", "label": "Hispanic or Latino"}, {"value": "2186-5", "label": "Not Hispanic or Latino"}, {"value": "UNK", "label": "Unknown"}, {"value": "ASKU", "label": "Asked but unknown"}],
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity].valueCode", "type": "code"}}
          ]
        },
        {
          "key": "contact-info",
          "title": "Contact Information",
          "columns": 2,
          "collapsible": false,
          "fields": [
            {"key": "phoneNumber", "label": "Mobile Phone", "type": "phone", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "telecom.where(system=''phone'').value", "type": "string"}},
            {"key": "homePhone", "label": "Home Phone", "type": "phone", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/home-phone].valueString", "type": "string"}},
            {"key": "email", "label": "Email Address", "type": "email", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "telecom.where(system=''email'').value", "type": "string"}},
            {"key": "preferredContactMethod", "label": "Preferred Contact Method", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "phone", "label": "Phone"}, {"value": "email", "label": "Email"}, {"value": "text", "label": "Text/SMS"}, {"value": "mail", "label": "Mail"}, {"value": "portal", "label": "Patient Portal"}],
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/preferred-contact-method].valueCode", "type": "code"}}
          ]
        },
        {
          "key": "address",
          "title": "Address",
          "columns": 2,
          "collapsible": false,
          "fields": [
            {"key": "address", "label": "Address", "type": "address", "required": false, "colSpan": 2,
             "fhirMapping": {"resource": "Patient", "path": "address[0]", "type": "address"}}
          ]
        },
        {
          "key": "emergency-contact",
          "title": "Emergency Contact",
          "columns": 2,
          "collapsible": false,
          "fields": [
            {"key": "emergencyName", "label": "Contact Name", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "contact[0].name.text", "type": "string"}},
            {"key": "emergencyRelationship", "label": "Relationship", "type": "select", "required": false, "colSpan": 1,
             "options": [{"value": "spouse", "label": "Spouse"}, {"value": "parent", "label": "Parent"}, {"value": "child", "label": "Child"}, {"value": "sibling", "label": "Sibling"}, {"value": "friend", "label": "Friend"}, {"value": "other", "label": "Other"}],
             "fhirMapping": {"resource": "Patient", "path": "contact[0].relationship[0].text", "type": "string"}},
            {"key": "emergencyPhone", "label": "Phone", "type": "phone", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "contact[0].telecom[0].value", "type": "string"}}
          ]
        },
        {
          "key": "communication-preferences",
          "title": "Communication Preferences",
          "columns": 2,
          "collapsible": false,
          "fields": [
            {"key": "allowSms", "label": "Allow SMS / Text Messages", "type": "toggle", "required": false, "colSpan": 1,
             "helpText": "Consent to receive text messages about your care",
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/hipaa-allow-sms].valueBoolean", "type": "boolean"}},
            {"key": "allowEmail", "label": "Allow Email Communication", "type": "toggle", "required": false, "colSpan": 1,
             "helpText": "Consent to receive health information via email",
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/hipaa-allow-email].valueBoolean", "type": "boolean"}},
            {"key": "allowVoicemail", "label": "Allow Voicemail", "type": "toggle", "required": false, "colSpan": 1,
             "helpText": "OK to leave health information in voicemail",
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/hipaa-allow-voicemail].valueBoolean", "type": "boolean"}},
            {"key": "allowMail", "label": "Allow Postal Mail", "type": "toggle", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/hipaa-allow-mail].valueBoolean", "type": "boolean"}}
          ]
        },
        {
          "key": "pharmacy",
          "title": "Preferred Pharmacy",
          "columns": 2,
          "collapsible": true,
          "collapsed": true,
          "fields": [
            {"key": "pharmacyName", "label": "Pharmacy Name", "type": "text", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/pharmacy-name].valueString", "type": "string"}},
            {"key": "pharmacyPhone", "label": "Pharmacy Phone", "type": "phone", "required": false, "colSpan": 1,
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/pharmacy-phone].valueString", "type": "string"}},
            {"key": "pharmacyAddress", "label": "Pharmacy Address", "type": "text", "required": false, "colSpan": 2,
             "fhirMapping": {"resource": "Patient", "path": "extension[url=http://ciyex.org/fhir/ext/pharmacy-address].valueString", "type": "string"}}
          ]
        }
      ]
    }',
    'Demographics', 'User', 'Portal', 0, true
) ON CONFLICT (tab_key, practice_type_code, org_id) DO UPDATE
SET field_config = EXCLUDED.field_config,
    fhir_resources = EXCLUDED.fhir_resources,
    label = EXCLUDED.label,
    icon = EXCLUDED.icon,
    category = EXCLUDED.category,
    updated_at = now();

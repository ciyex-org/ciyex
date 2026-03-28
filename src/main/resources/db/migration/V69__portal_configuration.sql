-- Portal configuration table — stores all portal settings per org as JSONB
CREATE TABLE portal_config (
    id          BIGSERIAL PRIMARY KEY,
    org_alias   VARCHAR(100) NOT NULL,
    config      JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias)
);

-- RLS: portal_config is org-scoped
ALTER TABLE portal_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY portal_config_rls ON portal_config
    USING (org_alias = current_setting('app.org_alias', true));

-- Portal form definitions table — stores configurable onboarding/consent forms
CREATE TABLE portal_form (
    id           BIGSERIAL PRIMARY KEY,
    org_alias    VARCHAR(100) NOT NULL,
    form_key     VARCHAR(100) NOT NULL,
    form_type    VARCHAR(50)  NOT NULL DEFAULT 'onboarding', -- onboarding, consent, intake, custom
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    field_config JSONB        NOT NULL DEFAULT '{"sections":[]}',
    settings     JSONB        NOT NULL DEFAULT '{}', -- { required, showOnRegistration, requireSignature, active, position }
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    position     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias, form_key)
);

ALTER TABLE portal_form ENABLE ROW LEVEL SECURITY;
CREATE POLICY portal_form_rls ON portal_form
    USING (org_alias = current_setting('app.org_alias', true));

CREATE INDEX idx_portal_form_org_type ON portal_form(org_alias, form_type);
CREATE INDEX idx_portal_form_active ON portal_form(org_alias, active);

-- Add "Portal Settings" to the layout settings area in EHR sidebar
-- First add a child menu item under the Settings parent
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position, parent_id)
SELECT m.id, 'portal-settings', 'Portal', 'Globe', '/settings/portal-settings', 92, NULL
FROM menu m WHERE m.code = 'ehr-sidebar'
ON CONFLICT DO NOTHING;

-- Insert default portal forms for all existing orgs (using sunrise as example)
-- These are configurable templates that admins can customize
INSERT INTO portal_form (org_alias, form_key, form_type, title, description, position, field_config, settings)
VALUES
(
    '__DEFAULT__', 'patient-onboarding', 'onboarding', 'Patient Onboarding',
    'Complete your profile to get started with the patient portal',
    0,
    '{
        "sections": [
            {
                "key": "personal",
                "title": "Personal Information",
                "columns": 2,
                "fields": [
                    { "key": "firstName", "label": "First Name", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "lastName", "label": "Last Name", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "dateOfBirth", "label": "Date of Birth", "type": "date", "required": true, "colSpan": 1 },
                    { "key": "gender", "label": "Gender", "type": "select", "required": true, "colSpan": 1, "options": [
                        { "value": "male", "label": "Male" },
                        { "value": "female", "label": "Female" },
                        { "value": "other", "label": "Other" },
                        { "value": "prefer-not-to-say", "label": "Prefer not to say" }
                    ]},
                    { "key": "phone", "label": "Phone Number", "type": "phone", "required": true, "colSpan": 1 },
                    { "key": "preferredLanguage", "label": "Preferred Language", "type": "select", "colSpan": 1, "options": [
                        { "value": "en", "label": "English" },
                        { "value": "es", "label": "Spanish" },
                        { "value": "fr", "label": "French" },
                        { "value": "zh", "label": "Chinese" },
                        { "value": "other", "label": "Other" }
                    ]}
                ]
            },
            {
                "key": "address",
                "title": "Address",
                "columns": 2,
                "fields": [
                    { "key": "streetAddress", "label": "Street Address", "type": "text", "required": true, "colSpan": 2 },
                    { "key": "city", "label": "City", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "state", "label": "State", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "zipCode", "label": "ZIP Code", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "country", "label": "Country", "type": "select", "colSpan": 1, "options": [
                        { "value": "US", "label": "United States" },
                        { "value": "CA", "label": "Canada" },
                        { "value": "other", "label": "Other" }
                    ]}
                ]
            },
            {
                "key": "emergency",
                "title": "Emergency Contact",
                "columns": 2,
                "fields": [
                    { "key": "emergencyName", "label": "Contact Name", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "emergencyRelation", "label": "Relationship", "type": "select", "required": true, "colSpan": 1, "options": [
                        { "value": "spouse", "label": "Spouse" },
                        { "value": "parent", "label": "Parent" },
                        { "value": "sibling", "label": "Sibling" },
                        { "value": "child", "label": "Child" },
                        { "value": "friend", "label": "Friend" },
                        { "value": "other", "label": "Other" }
                    ]},
                    { "key": "emergencyPhone", "label": "Contact Phone", "type": "phone", "required": true, "colSpan": 1 }
                ]
            },
            {
                "key": "insurance",
                "title": "Insurance Information",
                "columns": 2,
                "collapsible": true,
                "fields": [
                    { "key": "insuranceProvider", "label": "Insurance Provider", "type": "text", "colSpan": 1 },
                    { "key": "policyNumber", "label": "Policy Number", "type": "text", "colSpan": 1 },
                    { "key": "groupNumber", "label": "Group Number", "type": "text", "colSpan": 1 },
                    { "key": "subscriberName", "label": "Subscriber Name", "type": "text", "colSpan": 1 }
                ]
            }
        ]
    }'::jsonb,
    '{ "required": true, "showOnRegistration": true, "requireSignature": false }'::jsonb
),
(
    '__DEFAULT__', 'hipaa-consent', 'consent', 'HIPAA Notice of Privacy Practices',
    'Please review and sign our HIPAA privacy notice',
    1,
    '{
        "sections": [
            {
                "key": "notice",
                "title": "Notice of Privacy Practices",
                "columns": 1,
                "fields": [
                    { "key": "hipaaNotice", "label": "", "type": "computed", "colSpan": 1,
                      "helpText": "This notice describes how medical information about you may be used and disclosed and how you can get access to this information. Please review it carefully." },
                    { "key": "acknowledgeHipaa", "label": "I acknowledge that I have received and reviewed the Notice of Privacy Practices", "type": "checkbox", "required": true, "colSpan": 1 }
                ]
            },
            {
                "key": "authorization",
                "title": "Authorization for Treatment",
                "columns": 1,
                "fields": [
                    { "key": "authorizeTreatment", "label": "I authorize the healthcare providers to perform treatment and procedures as deemed necessary", "type": "checkbox", "required": true, "colSpan": 1 },
                    { "key": "authorizeInfoRelease", "label": "I authorize the release of medical information necessary for treatment, payment, or healthcare operations", "type": "checkbox", "required": true, "colSpan": 1 }
                ]
            },
            {
                "key": "signature",
                "title": "Signature",
                "columns": 2,
                "fields": [
                    { "key": "signatureName", "label": "Full Legal Name (Electronic Signature)", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "signatureDate", "label": "Date", "type": "date", "required": true, "colSpan": 1 },
                    { "key": "relationship", "label": "Relationship to Patient (if signing on behalf)", "type": "select", "colSpan": 1, "options": [
                        { "value": "self", "label": "Self" },
                        { "value": "parent", "label": "Parent/Guardian" },
                        { "value": "legal-rep", "label": "Legal Representative" },
                        { "value": "poa", "label": "Power of Attorney" }
                    ]}
                ]
            }
        ]
    }'::jsonb,
    '{ "required": true, "showOnRegistration": true, "requireSignature": true }'::jsonb
),
(
    '__DEFAULT__', 'telehealth-consent', 'consent', 'Telehealth Consent',
    'Consent for telehealth/virtual visit services',
    2,
    '{
        "sections": [
            {
                "key": "telehealth-info",
                "title": "Telehealth Services Information",
                "columns": 1,
                "fields": [
                    { "key": "telehealthInfo", "label": "", "type": "computed", "colSpan": 1,
                      "helpText": "Telehealth involves the use of electronic communications to enable healthcare providers to deliver care remotely. This may include assessment, diagnosis, consultation, treatment, education, and information transfer." },
                    { "key": "understandTelehealth", "label": "I understand that telehealth consultations are conducted remotely and may have limitations compared to in-person visits", "type": "checkbox", "required": true, "colSpan": 1 },
                    { "key": "consentTelehealth", "label": "I consent to participate in telehealth consultations as recommended by my healthcare provider", "type": "checkbox", "required": true, "colSpan": 1 },
                    { "key": "understandPrivacy", "label": "I understand that reasonable steps will be taken to ensure the privacy and security of my telehealth sessions", "type": "checkbox", "required": true, "colSpan": 1 }
                ]
            },
            {
                "key": "telehealth-signature",
                "title": "Signature",
                "columns": 2,
                "fields": [
                    { "key": "signatureName", "label": "Full Legal Name (Electronic Signature)", "type": "text", "required": true, "colSpan": 1 },
                    { "key": "signatureDate", "label": "Date", "type": "date", "required": true, "colSpan": 1 }
                ]
            }
        ]
    }'::jsonb,
    '{ "required": false, "showOnRegistration": false, "requireSignature": true }'::jsonb
);

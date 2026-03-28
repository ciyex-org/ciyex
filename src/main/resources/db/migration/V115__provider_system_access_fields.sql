-- V115: Update provider system access section to wire with Keycloak account creation
-- Replaces the old "username" and "rolesPermissions" fields with functional Keycloak-integrated fields

UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'system-access' THEN
          jsonb_build_object(
            'key', 'system-access',
            'title', 'System Access & Account',
            'columns', 3,
            'collapsible', true,
            'collapsed', false,
            'fields', '[
              {
                "key": "systemAccess.status",
                "label": "Provider Status",
                "type": "select",
                "required": true,
                "colSpan": 1,
                "options": [
                  {"value": "ACTIVE", "label": "Active"},
                  {"value": "ARCHIVED", "label": "Archived / Inactive"}
                ],
                "badgeColors": {"ACTIVE": "bg-green-100 text-green-800", "ARCHIVED": "bg-gray-100 text-gray-600"},
                "fhirMapping": {"resource": "Practitioner", "path": "active", "type": "boolean"}
              },
              {
                "key": "systemAccess.email",
                "label": "Login Email",
                "type": "email",
                "required": false,
                "colSpan": 1,
                "placeholder": "provider@clinic.com",
                "helperText": "Keycloak login email — account created on save"
              },
              {
                "key": "systemAccess.role",
                "label": "System Role",
                "type": "select",
                "required": false,
                "colSpan": 1,
                "defaultValue": "PROVIDER",
                "options": [
                  {"value": "PROVIDER", "label": "Provider"},
                  {"value": "ADMIN", "label": "Admin"},
                  {"value": "NURSE", "label": "Nurse"},
                  {"value": "MA", "label": "Medical Assistant"},
                  {"value": "FRONT_DESK", "label": "Front Desk"},
                  {"value": "BILLING", "label": "Billing"}
                ]
              }
            ]'::jsonb
          )
        ELSE section
      END
    )
    FROM jsonb_array_elements(field_config->'sections') AS section
  )
)
WHERE tab_key = 'providers';

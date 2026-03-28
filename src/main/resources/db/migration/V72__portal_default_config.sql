-- Seed default portal configuration into the database.
-- Navigation items include 'path' (route) and Lucide icon names so the frontend
-- renders menus entirely from config — no hardcoded icon maps or route tables.

INSERT INTO portal_config (org_alias, config)
VALUES ('__DEFAULT__', '{
  "general": {
    "portalName": "Patient Portal",
    "welcomeMessage": "Welcome to your patient portal",
    "registrationMode": "open",
    "sessionTimeout": 30,
    "allowSelfRegistration": true,
    "requireEmailVerification": false,
    "maintenanceMode": false
  },
  "features": {
    "appointments": { "enabled": true, "allowScheduling": true, "allowCancellation": true },
    "messaging":    { "enabled": true, "allowNewConversations": true },
    "labs":         { "enabled": true, "showResults": true },
    "medications":  { "enabled": true, "allowRefillRequests": false },
    "vitals":       { "enabled": true, "allowEntry": true },
    "documents":    { "enabled": true, "allowUpload": true },
    "billing":      { "enabled": true, "allowPayments": false },
    "insurance":    { "enabled": true, "allowEditing": true },
    "demographics": { "enabled": true, "allowEditing": true },
    "education":    { "enabled": true },
    "telehealth":   { "enabled": true },
    "reports":      { "enabled": true }
  },
  "navigation": [
    { "key": "dashboard",    "label": "Dashboard",         "icon": "LayoutDashboard", "path": "/dashboard",    "visible": true, "position": 0 },
    { "key": "demographics", "label": "Demographics",      "icon": "CircleUser",      "path": "/demographics", "visible": true, "position": 1 },
    { "key": "appointments", "label": "Appointments",      "icon": "Calendar",        "path": "/appointments", "visible": true, "position": 2 },
    { "key": "vitals",       "label": "Vitals",            "icon": "Activity",        "path": "/vitals",       "visible": true, "position": 3 },
    { "key": "medications",  "label": "Medications",       "icon": "Pill",            "path": "/medications",  "visible": true, "position": 4 },
    { "key": "allergies",    "label": "Allergies & History","icon": "TriangleAlert",   "path": "/allergies",    "visible": true, "position": 5 },
    { "key": "messages",     "label": "Messages",          "icon": "MessageSquare",   "path": "/messages",     "visible": true, "position": 6 },
    { "key": "education",    "label": "Patient Education", "icon": "BookOpen",        "path": "/education",    "visible": true, "position": 7 },
    { "key": "billing",      "label": "Billing",           "icon": "Receipt",         "path": "/billing",      "visible": true, "position": 8 },
    { "key": "insurance",    "label": "Insurance",         "icon": "Shield",          "path": "/insurance",    "visible": true, "position": 9 },
    { "key": "labs",         "label": "Labs",              "icon": "FlaskConical",    "path": "/labs",         "visible": true, "position": 10 },
    { "key": "reports",      "label": "Reports",           "icon": "ChartBar",       "path": "/reports",      "visible": true, "position": 11 },
    { "key": "documents",    "label": "Documents",         "icon": "FileText",        "path": "/documents",    "visible": true, "position": 12 },
    { "key": "telehealth",   "label": "Telehealth",        "icon": "Video",           "path": "/telehealth",   "visible": true, "position": 13 }
  ],
  "onboarding": { "enabled": true }
}'::jsonb)
ON CONFLICT (org_alias) DO NOTHING;

-- V139: Fix FHIR date field types across multiple tabs
-- Issues: date fields not fetched/displayed because FHIR type mismatch
-- Period.start/end expects DateTimeType, onset/abatement are choice types requiring dateTime

-- 1. Fix insurance-coverage: period.start and period.end should be dateTime (FHIR Period uses DateTimeType)
UPDATE tab_field_config
SET field_config = regexp_replace(
    regexp_replace(
        field_config::text,
        '"path"\s*:\s*"period\.start"\s*,\s*"type"\s*:\s*"date"',
        '"path":"period.start","type":"dateTime"',
        'g'
    ),
    '"path"\s*:\s*"period\.end"\s*,\s*"type"\s*:\s*"date"',
    '"path":"period.end","type":"dateTime"',
    'g'
)::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key = 'insurance-coverage'
  AND field_config::text LIKE '%period.start%'
  AND field_config::text LIKE '%"type":"date"%';

-- 2. Fix medicalproblems: onsetDateTime and abatementDateTime should use type dateTime
UPDATE tab_field_config
SET field_config = regexp_replace(
    regexp_replace(
        field_config::text,
        '"path"\s*:\s*"onsetDateTime"\s*,\s*"type"\s*:\s*"date"',
        '"path":"onsetDateTime","type":"dateTime"',
        'g'
    ),
    '"path"\s*:\s*"abatementDateTime"\s*,\s*"type"\s*:\s*"date"',
    '"path":"abatementDateTime","type":"dateTime"',
    'g'
)::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key = 'medicalproblems'
  AND field_config::text LIKE '%onsetDateTime%';

-- 3. Fix issues: onsetDateTime should use type dateTime
UPDATE tab_field_config
SET field_config = regexp_replace(
    field_config::text,
    '"path"\s*:\s*"onsetDateTime"\s*,\s*"type"\s*:\s*"date"',
    '"path":"onsetDateTime","type":"dateTime"',
    'g'
)::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key = 'issues'
  AND field_config::text LIKE '%onsetDateTime%';

-- 4. Fix issues: add showInTable flag for onsetDate
UPDATE tab_field_config
SET field_config = replace(
    field_config::text,
    '"key":"onsetDate","label":"Onset Date","type":"date"',
    '"key":"onsetDate","label":"Onset Date","type":"date","showInTable":true'
)::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key IN ('issues', 'medicalproblems')
  AND field_config::text LIKE '%"key":"onsetDate"%'
  AND field_config::text NOT LIKE '%"key":"onsetDate"%showInTable%';

-- 5. Fix relationships: ensure name fields and relationship type show in table
-- Also improve name mapping to use given[0] + family instead of just text
UPDATE tab_field_config
SET field_config = '{
  "sections": [
    {
      "key": "person-info",
      "title": "Related Person Information",
      "columns": 2,
      "fields": [
        {"key":"firstName","label":"First Name","type":"text","required":true,"colSpan":1,
         "fhirMapping":{"resource":"RelatedPerson","path":"name[0].given[0]","type":"string"},
         "showInTable":true},
        {"key":"lastName","label":"Last Name","type":"text","required":true,"colSpan":1,
         "fhirMapping":{"resource":"RelatedPerson","path":"name[0].family","type":"string"},
         "showInTable":true},
        {"key":"relationshipType","label":"Relationship Type","type":"select","required":true,"colSpan":1,
         "options":[{"value":"parent","label":"Parent"},{"value":"child","label":"Child"},{"value":"spouse","label":"Spouse"},{"value":"sibling","label":"Sibling"},{"value":"guardian","label":"Guardian"},{"value":"emergency","label":"Emergency Contact"},{"value":"other","label":"Other"}],
         "fhirMapping":{"resource":"RelatedPerson","path":"relationship[0].coding[0].code","type":"code"},
         "showInTable":true},
        {"key":"emergencyContact","label":"Emergency Contact","type":"checkbox","required":false,"colSpan":1,
         "fhirMapping":{"resource":"RelatedPerson","path":"extension[url=http://ciyex.org/fhir/ext/emergency-contact].valueBoolean","type":"boolean"},
         "showInTable":true},
        {"key":"phoneNumber","label":"Phone Number","type":"phone","required":false,"colSpan":1,
         "fhirMapping":{"resource":"RelatedPerson","path":"telecom[0].value","type":"string","system":"phone"},
         "showInTable":true},
        {"key":"email","label":"Email","type":"text","required":false,"colSpan":1,
         "fhirMapping":{"resource":"RelatedPerson","path":"telecom[1].value","type":"string","system":"email"}},
        {"key":"address","label":"Address","type":"text","required":false,"colSpan":2,
         "fhirMapping":{"resource":"RelatedPerson","path":"address[0].text","type":"string"}},
        {"key":"active","label":"Active","type":"select","required":false,"colSpan":1,
         "options":[{"value":"true","label":"Active"},{"value":"false","label":"Inactive"}],
         "fhirMapping":{"resource":"RelatedPerson","path":"active","type":"boolean"}},
        {"key":"notes","label":"Notes","type":"textarea","required":false,"colSpan":2,
         "fhirMapping":{"resource":"RelatedPerson","path":"extension[url=http://ciyex.org/fhir/ext/notes].valueString","type":"string"}}
      ]
    }
  ]
}'::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key = 'relationships' AND practice_type_code = '*' AND org_id = '*';

-- 6. Fix documents: ensure document date shows in table
UPDATE tab_field_config
SET field_config = regexp_replace(
    field_config::text,
    '"key"\s*:\s*"date"\s*,\s*"label"\s*:\s*"Document Date"\s*,\s*"type"\s*:\s*"datetime"',
    '"key":"date","label":"Document Date","type":"datetime","showInTable":true',
    'g'
)::jsonb,
version = version + 1,
updated_at = now()
WHERE tab_key = 'documents'
  AND field_config::text LIKE '%"key":"date"%'
  AND field_config::text NOT LIKE '%"key":"date"%showInTable%';

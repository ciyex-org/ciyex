-- V31: Replace text-based availability section with custom schedule editor component.
-- The availability section now renders ProviderAvailabilityEditor which creates
-- real FHIR Schedule resources used by appointment booking.

UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config,
    '{sections}',
    (
        SELECT jsonb_agg(
            CASE
                WHEN elem->>'key' = 'availability'
                THEN '{"key":"availability","title":"Availability & Scheduling","sectionComponent":"provider-availability-editor","columns":1,"collapsible":true,"collapsed":false,"fields":[]}'::jsonb
                ELSE elem
            END
        )
        FROM jsonb_array_elements(field_config->'sections') AS elem
    )
),
    updated_at = now()
WHERE tab_key = 'providers'
  AND practice_type_code = '*'
  AND org_id = '*'
  AND field_config->'sections' IS NOT NULL;

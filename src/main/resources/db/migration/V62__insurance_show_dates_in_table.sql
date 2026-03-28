-- Show effective date and end date in insurance coverage table
UPDATE tab_field_config
SET field_config = jsonb_set(
    field_config::jsonb,
    '{sections}',
    (
        SELECT jsonb_agg(
            CASE
                WHEN section->>'key' = 'policy-info' THEN
                    jsonb_set(
                        section,
                        '{fields}',
                        (
                            SELECT jsonb_agg(
                                CASE
                                    WHEN field->>'key' IN ('policyEffectiveDate', 'policyEndDate')
                                    THEN field || '{"showInTable": true}'::jsonb
                                    ELSE field
                                END
                            )
                            FROM jsonb_array_elements(section->'fields') AS field
                        )
                    )
                ELSE section
            END
        )
        FROM jsonb_array_elements(field_config::jsonb->'sections') AS section
    )
)
WHERE tab_key = 'insurance-coverage';

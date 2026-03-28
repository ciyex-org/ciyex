-- Change Insurance Company / Payer field from text to combobox with common payer options
-- Users can still type any custom company name (combobox supports free text)
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
                                    WHEN field->>'key' = 'payerName' THEN
                                        field || '{
                                            "type": "combobox",
                                            "options": [
                                                {"label": "Aetna", "value": "Aetna"},
                                                {"label": "Anthem Blue Cross", "value": "Anthem Blue Cross"},
                                                {"label": "Blue Cross Blue Shield", "value": "Blue Cross Blue Shield"},
                                                {"label": "Cigna", "value": "Cigna"},
                                                {"label": "Humana", "value": "Humana"},
                                                {"label": "Kaiser Permanente", "value": "Kaiser Permanente"},
                                                {"label": "Medicaid", "value": "Medicaid"},
                                                {"label": "Medicare", "value": "Medicare"},
                                                {"label": "Molina Healthcare", "value": "Molina Healthcare"},
                                                {"label": "Oscar Health", "value": "Oscar Health"},
                                                {"label": "UnitedHealthcare", "value": "UnitedHealthcare"},
                                                {"label": "Tricare", "value": "Tricare"},
                                                {"label": "Centene", "value": "Centene"},
                                                {"label": "Wellcare", "value": "Wellcare"},
                                                {"label": "Ambetter", "value": "Ambetter"},
                                                {"label": "Bright Health", "value": "Bright Health"},
                                                {"label": "Clover Health", "value": "Clover Health"},
                                                {"label": "Friday Health Plans", "value": "Friday Health Plans"},
                                                {"label": "WellPoint", "value": "WellPoint"}
                                            ]
                                        }'::jsonb
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

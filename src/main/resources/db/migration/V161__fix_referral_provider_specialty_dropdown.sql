-- Update referral-providers specialty field to be a select with common medical specialties
UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config::jsonb,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section ? 'fields' THEN
          jsonb_set(
            section,
            '{fields}',
            (
              SELECT jsonb_agg(
                CASE
                  WHEN field->>'key' = 'specialty' THEN
                    field || '{
                      "type": "combobox",
                      "options": [
                        "Allergy & Immunology",
                        "Anesthesiology",
                        "Cardiology",
                        "Dermatology",
                        "Emergency Medicine",
                        "Endocrinology",
                        "Family Medicine",
                        "Gastroenterology",
                        "General Surgery",
                        "Geriatrics",
                        "Hematology",
                        "Infectious Disease",
                        "Internal Medicine",
                        "Nephrology",
                        "Neurology",
                        "Neurosurgery",
                        "Obstetrics & Gynecology",
                        "Oncology",
                        "Ophthalmology",
                        "Oral Surgery",
                        "Orthopedic Surgery",
                        "Otolaryngology (ENT)",
                        "Pain Management",
                        "Pathology",
                        "Pediatrics",
                        "Physical Medicine & Rehabilitation",
                        "Plastic Surgery",
                        "Podiatry",
                        "Psychiatry",
                        "Pulmonology",
                        "Radiology",
                        "Rheumatology",
                        "Sports Medicine",
                        "Urology",
                        "Vascular Surgery"
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
WHERE tab_key = 'referral-providers'
  AND field_config::jsonb->'sections' IS NOT NULL;

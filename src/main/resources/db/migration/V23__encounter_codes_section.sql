-- V23: Fix ICD-10 code system name and add procedure coding section to encounter forms.
-- The codes service uses ICD10_CM (not ICD10) as the enum value.
-- Also adds a "Procedures & Coding" section with CPT/HCPCS lookups.

-- Fix all 4 encounter form configs: update ICD10 → ICD10_CM and add procedure-coding section

-- 1. Universal form
UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'assessment' THEN
          jsonb_set(section, '{fields}',
            jsonb_build_array(
              jsonb_build_object(
                'key', 'assessment_diagnoses', 'label', 'Diagnoses', 'type', 'diagnosis-list', 'colSpan', 1,
                'diagnosisConfig', jsonb_build_object('codeSystem', 'ICD10_CM', 'allowMultiple', true)
              ),
              jsonb_build_object('key', 'assessment_notes', 'label', 'Assessment Notes', 'type', 'textarea', 'colSpan', 1, 'placeholder', 'Clinical reasoning and assessment...')
            )
          )
        ELSE section
      END
    )
    FROM jsonb_array_elements(field_config->'sections') section
  ) || jsonb_build_array(
    jsonb_build_object(
      'key', 'procedure-coding',
      'title', 'Procedures & Coding',
      'columns', 1,
      'collapsible', true,
      'collapsed', false,
      'fields', jsonb_build_array(
        jsonb_build_object(
          'key', 'procedure_cpt_codes', 'label', 'CPT Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'CPT', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search CPT procedure codes...')
        ),
        jsonb_build_object(
          'key', 'procedure_hcpcs_codes', 'label', 'HCPCS Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'HCPCS', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search HCPCS codes...')
        ),
        jsonb_build_object('key', 'procedure_notes', 'label', 'Coding Notes', 'type', 'textarea', 'colSpan', 1, 'placeholder', 'Additional coding notes...')
      )
    )
  )
),
updated_at = now()
WHERE tab_key = 'encounter-form' AND practice_type_code = '*';

-- 2. Cardiology form
UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'assessment' THEN
          jsonb_set(section, '{fields}',
            jsonb_build_array(
              jsonb_build_object(
                'key', 'assessment_diagnoses', 'label', 'Diagnoses', 'type', 'diagnosis-list', 'colSpan', 1,
                'diagnosisConfig', jsonb_build_object('codeSystem', 'ICD10_CM', 'allowMultiple', true)
              ),
              jsonb_build_object('key', 'assessment_notes', 'label', 'Assessment Notes', 'type', 'textarea', 'colSpan', 1)
            )
          )
        ELSE section
      END
    )
    FROM jsonb_array_elements(field_config->'sections') section
  ) || jsonb_build_array(
    jsonb_build_object(
      'key', 'procedure-coding',
      'title', 'Procedures & Coding',
      'columns', 1,
      'collapsible', true,
      'collapsed', false,
      'fields', jsonb_build_array(
        jsonb_build_object(
          'key', 'procedure_cpt_codes', 'label', 'CPT Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'CPT', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search CPT procedure codes...')
        ),
        jsonb_build_object(
          'key', 'procedure_hcpcs_codes', 'label', 'HCPCS Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'HCPCS', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search HCPCS codes...')
        ),
        jsonb_build_object('key', 'procedure_notes', 'label', 'Coding Notes', 'type', 'textarea', 'colSpan', 1)
      )
    )
  )
),
updated_at = now()
WHERE tab_key = 'encounter-form' AND practice_type_code = 'cardiology';

-- 3. Psychiatry form
UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'assessment' THEN
          jsonb_set(section, '{fields}',
            jsonb_build_array(
              jsonb_build_object(
                'key', 'assessment_diagnoses', 'label', 'Diagnoses', 'type', 'diagnosis-list', 'colSpan', 1,
                'diagnosisConfig', jsonb_build_object('codeSystem', 'ICD10_CM', 'allowMultiple', true)
              ),
              jsonb_build_object('key', 'assessment_notes', 'label', 'Assessment / Formulation', 'type', 'textarea', 'colSpan', 1, 'placeholder', 'Clinical formulation, biopsychosocial factors...')
            )
          )
        ELSE section
      END
    )
    FROM jsonb_array_elements(field_config->'sections') section
  ) || jsonb_build_array(
    jsonb_build_object(
      'key', 'procedure-coding',
      'title', 'Procedures & Coding',
      'columns', 1,
      'collapsible', true,
      'collapsed', true,
      'fields', jsonb_build_array(
        jsonb_build_object(
          'key', 'procedure_cpt_codes', 'label', 'CPT Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'CPT', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search CPT E/M codes...')
        ),
        jsonb_build_object('key', 'procedure_notes', 'label', 'Coding Notes', 'type', 'textarea', 'colSpan', 1)
      )
    )
  )
),
updated_at = now()
WHERE tab_key = 'encounter-form' AND practice_type_code = 'psychiatry';

-- 4. Dermatology form
UPDATE tab_field_config
SET field_config = jsonb_set(
  field_config,
  '{sections}',
  (
    SELECT jsonb_agg(
      CASE
        WHEN section->>'key' = 'assessment' THEN
          jsonb_set(section, '{fields}',
            jsonb_build_array(
              jsonb_build_object(
                'key', 'assessment_diagnoses', 'label', 'Diagnoses', 'type', 'diagnosis-list', 'colSpan', 1,
                'diagnosisConfig', jsonb_build_object('codeSystem', 'ICD10_CM', 'allowMultiple', true)
              ),
              jsonb_build_object('key', 'assessment_notes', 'label', 'Assessment Notes', 'type', 'textarea', 'colSpan', 1)
            )
          )
        ELSE section
      END
    )
    FROM jsonb_array_elements(field_config->'sections') section
  ) || jsonb_build_array(
    jsonb_build_object(
      'key', 'procedure-coding',
      'title', 'Procedures & Coding',
      'columns', 1,
      'collapsible', true,
      'collapsed', false,
      'fields', jsonb_build_array(
        jsonb_build_object(
          'key', 'procedure_cpt_codes', 'label', 'CPT Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'CPT', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search CPT procedure codes...')
        ),
        jsonb_build_object(
          'key', 'procedure_hcpcs_codes', 'label', 'HCPCS Codes', 'type', 'code-lookup', 'colSpan', 1,
          'codeLookupConfig', jsonb_build_object('codeSystem', 'HCPCS', 'allowMultiple', true, 'showFee', true, 'placeholder', 'Search HCPCS codes...')
        ),
        jsonb_build_object('key', 'procedure_notes', 'label', 'Coding Notes', 'type', 'textarea', 'colSpan', 1)
      )
    )
  )
),
updated_at = now()
WHERE tab_key = 'encounter-form' AND practice_type_code = 'dermatology';

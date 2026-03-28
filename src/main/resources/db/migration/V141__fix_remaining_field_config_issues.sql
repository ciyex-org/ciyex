-- V141: Fix remaining field config issues
-- 1. Fix issues tab: only Condition resource (not AllergyIntolerance/MedicationRequest)
-- 2. Fix transactions.amount type from 'quantity' to 'decimal'
-- 3. Fix messaging.sender type from 'Reference' to 'reference'
-- 4. Add claim-denials missing showInTable fields (responseDate, originalClaimRef)
-- 5. Fix labs.effectiveDate type from 'datetime' to 'dateTime'
-- 6. Fix medications.dateIssued type from 'datetime' to 'dateTime'

-- 1. Fix issues tab to only use Condition (not AllergyIntolerance/MedicationRequest)
UPDATE tab_field_config
SET fhir_resources = '[{"type": "Condition", "patientSearchParam": "subject"}]'::jsonb
WHERE tab_key = 'issues' AND org_id = '*' AND practice_type_code = '*';

-- 2. Fix transactions.amount fhirMapping type from 'quantity' to 'decimal'
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            jsonb_set(section, '{fields}',
                (SELECT jsonb_agg(
                    CASE
                        WHEN field->>'key' = 'amount' AND field->'fhirMapping'->>'type' = 'quantity'
                        THEN jsonb_set(field, '{fhirMapping,type}', '"decimal"')
                        ELSE field
                    END
                ) FROM jsonb_array_elements(section->'fields') field)
            )
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'transactions' AND org_id = '*' AND practice_type_code = '*';

-- 3. Fix messaging.sender fhirMapping type from 'Reference' to 'reference'
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            jsonb_set(section, '{fields}',
                (SELECT jsonb_agg(
                    CASE
                        WHEN field->>'key' = 'sender' AND field->'fhirMapping'->>'type' = 'Reference'
                        THEN jsonb_set(field, '{fhirMapping,type}', '"reference"')
                        ELSE field
                    END
                ) FROM jsonb_array_elements(section->'fields') field)
            )
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'messaging' AND org_id = '*' AND practice_type_code = '*';

-- 4. Add responseDate and originalClaimRef fields to claim-denials if missing, with showInTable
-- First add responseDate field
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            CASE
                WHEN section->>'key' = (SELECT s->>'key' FROM jsonb_array_elements(field_config->'sections') s LIMIT 1)
                AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(section->'fields') f WHERE f->>'key' = 'responseDate')
                THEN jsonb_set(section, '{fields}', section->'fields' || '[{"key":"responseDate","type":"date","label":"Response Date","showInTable":true,"fhirMapping":{"path":"payment.date","type":"date","resource":"ClaimResponse"}}]'::jsonb)
                ELSE section
            END
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'claim-denials' AND org_id = '*' AND practice_type_code = '*';

-- Add originalClaimRef field
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            CASE
                WHEN section->>'key' = (SELECT s->>'key' FROM jsonb_array_elements(field_config->'sections') s LIMIT 1)
                AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(section->'fields') f WHERE f->>'key' = 'originalClaimRef')
                THEN jsonb_set(section, '{fields}', section->'fields' || '[{"key":"originalClaimRef","type":"text","label":"Original Claim Ref","showInTable":true,"fhirMapping":{"path":"request.reference","type":"reference","resource":"ClaimResponse"}}]'::jsonb)
                ELSE section
            END
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'claim-denials' AND org_id = '*' AND practice_type_code = '*';

-- Make paymentDate showInTable for claim-denials (it's the response date)
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            jsonb_set(section, '{fields}',
                (SELECT jsonb_agg(
                    CASE
                        WHEN field->>'key' = 'paymentDate'
                        THEN field || '{"showInTable": true}'::jsonb
                        WHEN field->>'key' = 'request'
                        THEN field || '{"showInTable": true}'::jsonb
                        ELSE field
                    END
                ) FROM jsonb_array_elements(section->'fields') field)
            )
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'claim-denials' AND org_id = '*' AND practice_type_code = '*';

-- 5. Fix labs.effectiveDate type from 'datetime' to 'dateTime'
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            jsonb_set(section, '{fields}',
                (SELECT jsonb_agg(
                    CASE
                        WHEN field->>'key' = 'effectiveDate' AND field->'fhirMapping'->>'type' = 'datetime'
                        THEN jsonb_set(field, '{fhirMapping,type}', '"dateTime"')
                        ELSE field
                    END
                ) FROM jsonb_array_elements(section->'fields') field)
            )
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'labs' AND org_id = '*' AND practice_type_code = '*';

-- 6. Fix medications.dateIssued type from 'datetime' to 'dateTime'
UPDATE tab_field_config
SET field_config = (
    SELECT jsonb_set(field_config, '{sections}',
        (SELECT jsonb_agg(
            jsonb_set(section, '{fields}',
                (SELECT jsonb_agg(
                    CASE
                        WHEN field->>'key' = 'dateIssued' AND field->'fhirMapping'->>'type' = 'datetime'
                        THEN jsonb_set(field, '{fhirMapping,type}', '"dateTime"')
                        ELSE field
                    END
                ) FROM jsonb_array_elements(section->'fields') field)
            )
        ) FROM jsonb_array_elements(field_config->'sections') section)
    )
)
WHERE tab_key = 'medications' AND org_id = '*' AND practice_type_code = '*';

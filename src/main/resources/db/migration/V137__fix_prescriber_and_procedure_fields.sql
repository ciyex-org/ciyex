-- V137: Fix prescriber field path and procedure field showInTable flags
-- 1. Fix prescriber FHIR path: requester.reference → requester (allows reading both .reference and .display)
-- 2. Add showInTable to key procedure fields (performedDate, performer)
-- 3. Add showInTable to key medication fields (medicationName, status, dosage, prescriber, dateIssued)

-- ── 1. Fix prescriber FHIR path in medications tab ──
-- The old V91 migration changed from requester.display → requester.reference,
-- but existing records may only have .display set. Using just "requester" allows
-- FhirPathMapper.extractPrimitiveValue to handle both .reference and .display.
UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"path":"requester.reference"',
    '"path":"requester"'
)::jsonb,
updated_at = now()
WHERE tab_key = 'medications'
  AND field_config::text LIKE '%requester.reference%';

-- ── 2. Add showInTable to procedure fields ──
-- Add showInTable:true to procedureName, status, performedDate, performer
UPDATE tab_field_config
SET field_config = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                field_config::text,
                '"key":"procedureName","label":"Procedure","type":"text","required":true,"colSpan":1',
                '"key":"procedureName","label":"Procedure","type":"text","required":true,"colSpan":1,"showInTable":true'
            ),
            '"key":"status","label":"Status","type":"select","required":true,"colSpan":1',
            '"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"showInTable":true'
        ),
        '"key":"performedDate","label":"Date Performed","type":"datetime","required":false,"colSpan":1',
        '"key":"performedDate","label":"Date Performed","type":"datetime","required":false,"colSpan":1,"showInTable":true'
    ),
    '"key":"performer","label":"Performer","type":"lookup","required":false,"colSpan":1',
    '"key":"performer","label":"Performer","type":"lookup","required":false,"colSpan":1,"showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'procedures'
  AND org_id = '*'
  AND practice_type_code = '*';

-- ── 3. Add showInTable to medication fields ──
UPDATE tab_field_config
SET field_config = REPLACE(
    REPLACE(
        REPLACE(
            field_config::text,
            '"key":"medicationName","label":"Medication Name","type":"text","required":true,"colSpan":1',
            '"key":"medicationName","label":"Medication Name","type":"text","required":true,"colSpan":1,"showInTable":true'
        ),
        '"key":"status","label":"Status","type":"select","required":true,"colSpan":1',
        '"key":"status","label":"Status","type":"select","required":true,"colSpan":1,"showInTable":true'
    ),
    '"key":"prescribingDoctor","label":"Prescriber","type":"lookup","required":false,"colSpan":1',
    '"key":"prescribingDoctor","label":"Prescriber","type":"lookup","required":false,"colSpan":1,"showInTable":true'
)::jsonb,
updated_at = now()
WHERE tab_key = 'medications'
  AND org_id = '*'
  AND practice_type_code = '*';

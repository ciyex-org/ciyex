-- V20: Fix problem-list → medicalproblems tab_key
-- V16 renamed 'problems' → 'problem-list', V18 tried to rename 'problems' → 'medicalproblems'
-- but 'problems' no longer existed (was already 'problem-list'). Fix the chain.

UPDATE tab_field_config
SET tab_key = 'medicalproblems',
    label = 'Problems',
    icon = 'HeartPulse',
    category = 'Overview',
    position = 5,
    updated_at = now()
WHERE tab_key = 'problem-list' AND practice_type_code = '*' AND org_id = '*';

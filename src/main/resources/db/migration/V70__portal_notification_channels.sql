-- V70: Add metadata JSONB to message table for structured notification data
-- and create system notification channels per org

-- 1. Add metadata column to message table for structured event data
ALTER TABLE message ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- 2. Add notification_type column for filtering notification messages
ALTER TABLE message ADD COLUMN IF NOT EXISTS notification_type VARCHAR(100);

-- 3. Index for querying notifications by type
CREATE INDEX IF NOT EXISTS idx_message_notification_type
    ON message (notification_type) WHERE notification_type IS NOT NULL;

-- 4. Index for finding channels by name within an org (for system channel lookup)
CREATE INDEX IF NOT EXISTS idx_channel_org_name
    ON channel (org_alias, name) WHERE is_archived = false;

-- 5. Create system notification channels for each existing org
-- These are public channels that all staff can see
INSERT INTO channel (id, name, type, topic, description, created_by, org_alias, is_archived, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'patient-updates',
    'public',
    'Patient portal updates and notifications',
    'Automatic notifications when patients update their demographics, insurance, upload documents, sign consent forms, or make other changes through the patient portal.',
    'system',
    oa.org_alias,
    false,
    NOW(),
    NOW()
FROM (SELECT DISTINCT org_alias FROM channel WHERE org_alias IS NOT NULL) oa
WHERE NOT EXISTS (
    SELECT 1 FROM channel c
    WHERE c.org_alias = oa.org_alias AND c.name = 'patient-updates' AND c.is_archived = false
);

-- 6. Create portal-activity channel for each org (appointment requests, messages, etc.)
INSERT INTO channel (id, name, type, topic, description, created_by, org_alias, is_archived, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'portal-activity',
    'public',
    'Patient portal activity feed',
    'Real-time feed of patient portal activity including appointment requests, message sends, document views, and form submissions.',
    'system',
    oa.org_alias,
    false,
    NOW(),
    NOW()
FROM (SELECT DISTINCT org_alias FROM channel WHERE org_alias IS NOT NULL) oa
WHERE NOT EXISTS (
    SELECT 1 FROM channel c
    WHERE c.org_alias = oa.org_alias AND c.name = 'portal-activity' AND c.is_archived = false
);

-- 7. RLS policies for the new columns follow the existing message table policies
-- (already covered by the V68 migration's RLS on the message table)

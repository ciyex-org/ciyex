-- =============================================
-- V68: Messaging - Channels, Messages, Reactions
-- Slack-style messaging system
-- =============================================

-- Channels (public, private, DM, group DM)
CREATE TABLE channel (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    type            VARCHAR(20)     NOT NULL DEFAULT 'public'
                        CHECK (type IN ('public', 'private', 'dm', 'group_dm')),
    topic           VARCHAR(500),
    description     TEXT,
    created_by      VARCHAR(255)    NOT NULL,
    org_alias       VARCHAR(100)    NOT NULL,
    is_archived     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_channel_org     ON channel (org_alias);
CREATE INDEX idx_channel_type    ON channel (org_alias, type);

-- Channel members (who belongs to which channel)
CREATE TABLE channel_member (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id      UUID            NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    user_id         VARCHAR(255)    NOT NULL,
    display_name    VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'member'
                        CHECK (role IN ('owner', 'admin', 'member')),
    last_read_at    TIMESTAMPTZ,
    is_muted        BOOLEAN         NOT NULL DEFAULT FALSE,
    org_alias       VARCHAR(100)    NOT NULL,
    joined_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_channel_member_user     ON channel_member (user_id, org_alias);
CREATE INDEX idx_channel_member_channel  ON channel_member (channel_id);

-- Messages
CREATE TABLE message (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id      UUID            NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    sender_id       VARCHAR(255)    NOT NULL,
    sender_name     VARCHAR(255)    NOT NULL,
    content         TEXT            NOT NULL,
    content_html    TEXT,
    parent_id       UUID            REFERENCES message(id) ON DELETE SET NULL,
    is_pinned       BOOLEAN         NOT NULL DEFAULT FALSE,
    is_edited       BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    system_type     VARCHAR(50),
    mentions        JSONB           DEFAULT '[]'::jsonb,
    org_alias       VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_channel     ON message (channel_id, created_at);
CREATE INDEX idx_message_parent      ON message (parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_message_pinned      ON message (channel_id) WHERE is_pinned = TRUE;
CREATE INDEX idx_message_org         ON message (org_alias);
CREATE INDEX idx_message_content_gin ON message USING gin (to_tsvector('english', content));

-- Message reactions
CREATE TABLE message_reaction (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID            NOT NULL REFERENCES message(id) ON DELETE CASCADE,
    user_id         VARCHAR(255)    NOT NULL,
    emoji           VARCHAR(50)     NOT NULL,
    org_alias       VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX idx_reaction_message ON message_reaction (message_id);

-- Message attachments
CREATE TABLE message_attachment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID            NOT NULL REFERENCES message(id) ON DELETE CASCADE,
    file_name       VARCHAR(500)    NOT NULL,
    file_url        VARCHAR(2000)   NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT          NOT NULL DEFAULT 0,
    thumbnail_url   VARCHAR(2000),
    org_alias       VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachment_message ON message_attachment (message_id);

-- Enable RLS
ALTER TABLE channel ENABLE ROW LEVEL SECURITY;
ALTER TABLE channel_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE message ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_reaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_attachment ENABLE ROW LEVEL SECURITY;

-- RLS policies (org_alias scoping)
CREATE POLICY channel_rls ON channel
    USING (org_alias = current_setting('app.current_tenant', true));
CREATE POLICY channel_member_rls ON channel_member
    USING (org_alias = current_setting('app.current_tenant', true));
CREATE POLICY message_rls ON message
    USING (org_alias = current_setting('app.current_tenant', true));
CREATE POLICY message_reaction_rls ON message_reaction
    USING (org_alias = current_setting('app.current_tenant', true));
CREATE POLICY message_attachment_rls ON message_attachment
    USING (org_alias = current_setting('app.current_tenant', true));

-- Messaging menu item already exists (added in earlier migration)

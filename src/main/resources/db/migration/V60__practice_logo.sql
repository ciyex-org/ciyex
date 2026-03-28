-- Practice logo storage (one logo per organization)
CREATE TABLE IF NOT EXISTS practice_logo (
    id          BIGSERIAL PRIMARY KEY,
    org_id      VARCHAR(100) NOT NULL,
    logo_data   TEXT         NOT NULL,   -- base64-encoded image data (data:image/...)
    content_type VARCHAR(50),            -- e.g. image/png, image/jpeg
    file_name   VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (org_id)
);

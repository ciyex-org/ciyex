-- V153: Create local table for FHIR resource form data.
-- Previously stored as a FHIR extension on the resource, but the FHIR server
-- rejects unknown extension URLs. Local storage avoids this validation issue.

CREATE TABLE IF NOT EXISTS fhir_resource_form_data (
    id              BIGSERIAL PRIMARY KEY,
    resource_type   VARCHAR(64)  NOT NULL,
    resource_id     VARCHAR(128) NOT NULL,
    org_alias       VARCHAR(128) NOT NULL,
    form_data       JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (resource_type, resource_id, org_alias)
);

CREATE INDEX idx_fhir_form_data_lookup
    ON fhir_resource_form_data (resource_type, resource_id, org_alias);

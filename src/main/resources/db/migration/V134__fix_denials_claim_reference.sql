-- Fix claim-denials: change ClaimResponse.request from type=reference to type=string
-- to prevent HAPI FHIR from validating that the referenced Claim exists on the server.
-- The Claim reference is stored as a plain string value (e.g. "Claim/4423").

UPDATE tab_field_config
SET field_config = REPLACE(
    field_config::text,
    '"fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"reference"}',
    '"fhirMapping":{"resource":"ClaimResponse","path":"request.reference","type":"string"}'
)::jsonb,
updated_at = now()
WHERE tab_key IN ('claim-denials', 'claim-submissions')
  AND field_config::text LIKE '%"path":"request.reference","type":"reference"%';

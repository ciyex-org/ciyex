package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request body for invoking CDS Hooks at a given hook point.
 * Sent from the EHR-UI to ciyex-api, which then fans out to all CDS services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsHookRequest {
    /** The hook being triggered (e.g., "patient-view", "order-sign") */
    private String hook;
    /** FHIR server base URL for the CDS service to call back */
    private String fhirServer;
    /** OAuth2 access token for FHIR access (passed to CDS services) */
    private String fhirAuthorization;
    /** Hook context data per CDS Hooks spec */
    private Map<String, Object> context;
    /** Pre-fetched FHIR resources (optional — avoids CDS service needing to call back) */
    private Map<String, Object> prefetch;

    @Builder.Default
    private String hookInstance = UUID.randomUUID().toString();
}

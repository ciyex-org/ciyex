package org.ciyex.ehr.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Describes a single CDS service as returned by the CDS Hooks discovery endpoint.
 * Per CDS Hooks 1.0 specification: GET {baseUrl}/cds-services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdsServiceDescriptor {
    /** Unique ID within the CDS service provider */
    private String id;
    /** The hook this service runs on (e.g., "patient-view", "order-sign") */
    private String hook;
    /** Human-readable title */
    private String title;
    /** Description of what this service does */
    private String description;
    /** Prefetch templates — FHIR queries the EHR should pre-fetch */
    private Map<String, String> prefetch;
    /** Whether this service uses patient FHIR context */
    private Boolean usesPatientContext;

    // Added by the EHR to track which app provides this service
    private String appSlug;
    private String appName;
    private String appIconUrl;
}

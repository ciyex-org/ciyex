package org.ciyex.ehr.dto.integration;

import lombok.Data;

@Data
public class FhirConfig {
    private String apiUrl;
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String scope;
}

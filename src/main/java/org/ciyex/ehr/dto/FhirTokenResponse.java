package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class FhirTokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String rawResponse;
}
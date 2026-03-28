package org.ciyex.ehr.dto.portal;

import lombok.Data;

@Data
public class PortalLoginRequest {
    private String email;
    private String password;
    private String orgAlias;  // FHIR partition alias; required when portal is not org-specific
}
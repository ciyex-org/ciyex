package org.ciyex.ehr.usermgmt.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String roleName;           // e.g. ADMIN, PROVIDER, NURSE
    private String temporaryPassword;  // optional — auto-generated if null
    private boolean sendWelcomeEmail;
    private boolean generatePrintCredentials;
    private String linkedFhirId;       // FHIR Practitioner or Patient ID to link
}

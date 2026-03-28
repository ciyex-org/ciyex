package org.ciyex.ehr.usermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean enabled;
    private boolean emailVerified;
    private List<String> roles;
    private List<String> groups;
    private Long createdTimestamp;
    private String temporaryPassword; // only populated on create/reset
    private String practitionerFhirId; // FHIR Practitioner resource ID (linked via KC attribute)
    private String npi; // NPI from KC attribute
}

package com.qiaben.ciyex.dto.portal;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class PortalLoginResponse {
    private String token;
    private Long userId;
    private String fhirId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String street;
    private String street2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private LocalDate dateOfBirth;
    private String profileImage;
    private List<OrgInfo> orgs;
    private AuditDto audit;

    @Data
    public static class OrgInfo {
        private String orgName;
        private String role;

        public OrgInfo(String orgName, String role) {
            this.orgName = orgName;
            this.role = role;
        }
    }

    // Setters for FHIR compatibility
    public void setId(Long id) { this.userId = id; }
    public void setPhoneNumber(String phone) { this.phone = phone; }
    public void setStatus(String status) { /* ignored for now */ }
}

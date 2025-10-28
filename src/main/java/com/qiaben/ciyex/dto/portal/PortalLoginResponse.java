package com.qiaben.ciyex.dto.portal;

import java.time.LocalDate;
import java.util.List;

import com.qiaben.ciyex.entity.portal.PortalUser;

import lombok.Data;
//
@Data
public class PortalLoginResponse {
    private String token;
    private Long userId;
    private String uuid;
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

    @Data
    public static class OrgInfo {
        private String orgName;
        private String role;

        public OrgInfo(String orgName, String role) {
            this.orgName = orgName;
            this.role = role;
        }
    }

    /**
     * ✅ Factory method to build PortalLoginResponse from PortalUser
     */
    public static PortalLoginResponse fromEntity(PortalUser user) {
        PortalLoginResponse resp = new PortalLoginResponse();
        resp.setUserId(user.getId());
        resp.setUuid(user.getUuid() != null ? user.getUuid().toString() : null);
        resp.setEmail(user.getEmail());
        resp.setFirstName(user.getFirstName());
        resp.setLastName(user.getLastName());
        resp.setPhone(user.getPhoneNumber());
        // Note: Other address fields (street, city, etc.) should come from PortalPatient entity
        return resp;
    }
}

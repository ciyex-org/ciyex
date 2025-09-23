package com.qiaben.ciyex.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.qiaben.ciyex.entity.portal.PortalUser;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortalUserDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String city;
    private String state;
    private String country;
    private String street;
    private String street2;
    private String postalCode;
    private String profileImage;
    private String securityQuestion;
    private String securityAnswer;
    private String uuid;

    private Long orgId;      // ✅ always return orgId
    private String orgName;  // ✅ always return orgName

    private String role;     // "PATIENT"

    public static PortalUserDto fromEntity(PortalUser user) {
        return PortalUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .street(user.getStreet())
                .street2(user.getStreet2())
                .postalCode(user.getPostalCode())
                .profileImage(user.getProfileImage())
                .securityQuestion(user.getSecurityQuestion())
                .securityAnswer(user.getSecurityAnswer())
                .uuid(user.getUuid() != null ? user.getUuid().toString() : null)
                .orgId(user.getOrgId())
                .role(user.getRole())
                .build();
    }

    // 🔹 Setter utility so services can attach orgName dynamically
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
}

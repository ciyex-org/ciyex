package com.qiaben.ciyex.dto.portal;

import lombok.*;
import java.time.LocalDate;
//
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalRegisterRequest {

    private String email;
    private String password;

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
    private Long orgId;   // 🔹 Added orgId for organization association
}

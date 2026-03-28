package org.ciyex.ehr.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private String orgName;
    private String role;
}

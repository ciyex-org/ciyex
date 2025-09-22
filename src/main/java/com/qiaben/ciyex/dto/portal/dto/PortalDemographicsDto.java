package com.qiaben.ciyex.dto.portal.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PortalDemographicsDto {
    private Long id;

    // Identity
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private String sex;
    private String maritalStatus;

    // Contact
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneMobile;
    private String contactEmail;

    // Emergency
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Preferences
    private boolean allowSMS;
    private boolean allowEmail;
    private boolean allowVoiceMessage;
    private boolean allowMailMessage;
}

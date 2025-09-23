package com.qiaben.ciyex.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalPatientDto {

    private Long id;
    private Long userId;   // Link to PortalUser.id

    private String firstName;
    private String lastName;
    private LocalDate dob;

    private String gender;
    private String phone;
    private String email;

    private String address;

    // Address details coming from PortalUser
    private String city;
    private String state;
    private String country;
    private String postalCode;
}

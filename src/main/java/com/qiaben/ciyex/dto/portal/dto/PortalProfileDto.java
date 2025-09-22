package com.qiaben.ciyex.dto.portal.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalProfileDto {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}

package org.ciyex.ehr.dto.portal;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;
//
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalProfileDto {
    private Long id;
    private UUID userId;
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

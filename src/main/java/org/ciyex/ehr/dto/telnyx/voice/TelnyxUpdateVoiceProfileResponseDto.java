package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUpdateVoiceProfileResponseDto {
    private String brandId;
    private String tcrBrandId;
    private String displayName;
    private String entityType;
    private String companyName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String status;
    private String createdAt;
    private String updatedAt;
}

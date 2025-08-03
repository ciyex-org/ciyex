package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class LocationDto {
    private Long id; // Database ID
    private String externalId; // ID from external storage (e.g., FHIR Location ID)
    private Long orgId;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}

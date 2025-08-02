package com.qiaben.ciyex.dto.core;

import lombok.Data;

@Data
public class LocationDto {
    private String externalId; // ID from external storage (e.g., FHIR Location ID)
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}

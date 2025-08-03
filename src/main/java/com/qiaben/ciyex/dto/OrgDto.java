package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class OrgDto {
    private Long id; // Included for response; null on create
    private String orgName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String fhirId; // Included for response
}

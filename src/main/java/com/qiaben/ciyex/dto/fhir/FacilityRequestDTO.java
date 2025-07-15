package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class FacilityRequestDTO {
    private String name;
    private String facilityNpi;
    private String phone;
    private String fax;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String countryCode;
    private String federalEin;
    private String website;
    private String email;
    private String domainIdentifier;
    private String facilityTaxonomy;
    private String facilityCode;
    private String billingLocation;
    private String acceptsAssignment;
    private String oid;
    private String serviceLocation;
}

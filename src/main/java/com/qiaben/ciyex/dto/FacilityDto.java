package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityDto {

    private Long id;
    private String name;

    // Physical Address
    private String physicalAddress;
    private String physicalCity;
    private String physicalState;
    private String physicalZipCode;
    private String physicalCountry;

    // Mailing Address
    private String mailingAddress;
    private String mailingCity;
    private String mailingState;
    private String mailingZipCode;
    private String mailingCountry;

    // Contact Information
    private String phone;
    private String fax;
    private String website;
    private String email;

    // Facility Details
    private String color;
    private String iban;
    private String posCode;
    private String facilityTaxonomy;
    private String cliaNumber;
    private String taxIdType;
    private String taxId;
    private String billingAttn;
    private String facilityLabCode;
    private String npi;
    private String oid;

    // Checkboxes
    private Boolean billingLocation;
    private Boolean acceptsAssignment;
    private Boolean serviceLocation;
    private Boolean primaryBusinessEntity;
    private Boolean facilityInactive;

    // Additional Info
    private String info;
    private Boolean isActive;

    // External ID
    private String externalId;
    private String fhirId;

    // Audit Information
    private Audit audit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
        private String createdBy;
        private String lastModifiedBy;
    }
}
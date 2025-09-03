package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class CoverageDto {
    private Long id;
    private String externalId;
    private String coverageType;
    private String planName;
    private String policyNumber;
    private String coverageStartDate;
    private String coverageEndDate;
    private Long patientId;
    private Long orgId;

    // Additional fields from the screenshot
    private String provider;
    private String effectiveDate;
    private String effectiveDateEnd;
    private String groupNumber;
    private String subscriberEmployer;
    private String subscriberAddressLine1;
    private String subscriberAddressLine2;
    private String subscriberCity;
    private String subscriberState;
    private String subscriberZipCode;
    private String subscriberCountry;
    private String subscriberPhone;


    private InsuranceCompanyDto insuranceCompany;  // Added InsuranceCompany DTO


    private String byholderName;
    private String byholderRelation;
    private String byholderAddressLine1;
    private String byholderAddressLine2;
    private String byholderCity;
    private String byholderState;
    private String byholderZipCode;
    private String byholderCountry;
    private String byholderPhone;
    private Double copayAmount;




}

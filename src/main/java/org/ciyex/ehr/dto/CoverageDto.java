package org.ciyex.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoverageDto {
    private Long id;
    private String fhirId;
    private String externalId;  // Alias for fhirId
    
    @NotBlank(message = "Coverage type is required")
    private String coverageType;
    
    @NotBlank(message = "Plan name is required")
    private String planName;
    
    @NotBlank(message = "Policy number is required")
    private String policyNumber;
    private String coverageStartDate;
    private String coverageEndDate;
    private Long patientId;

    // Additional fields from the screenshot
    @NotBlank(message = "Provider is required")
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
    
    @NotNull(message = "Copay amount is required")
    private Double copayAmount;

    private String cardFrontUrl;
    private String cardBackUrl;
    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;

}

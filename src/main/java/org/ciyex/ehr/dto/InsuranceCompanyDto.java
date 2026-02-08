package org.ciyex.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
//
@Data
public class InsuranceCompanyDto {
    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Address is mandatory")
    private String address;

    @NotBlank(message = "City is mandatory")
    private String city;

    @NotBlank(message = "State is mandatory")
    private String state;

    @NotBlank(message = "Postal code is mandatory")
    private String postalCode;

    @NotBlank(message = "Payer ID is mandatory")
    private String payerId;

    @NotBlank(message = "Country is mandatory")
    private String country;

    private String fhirId;
    private String externalId; // Alias for fhirId for external integrations
    
    @NotBlank(message = "Status is mandatory")
    private String status;  // ACTIVE or ARCHIVED

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;
}
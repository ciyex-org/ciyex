package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HealthcareServiceDto {
    private Long id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotBlank(message = "Type is required")
    private String type; // Organization ID passed in header
    
    @NotBlank(message = "Hours of operation is required")
    private String hoursOfOperation;
    
    private String externalId; // External ID from request
    private String fhirId; // FHIR ID in response
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

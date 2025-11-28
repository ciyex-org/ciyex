package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class HealthcareServiceDto {
    private Long id;
    private String name;
    private String description;
    private String location;
    private String type; // Organization ID passed in header
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

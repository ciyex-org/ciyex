package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AllergyIntoleranceDto {

    private String fhirId;           // FHIR resource ID
    private String externalId;       // Alias for fhirId
    private Long patientId;          // EHR patient id (omitted in API responses)
    private List<AllergyItem> allergiesList;
    private Audit audit;

    @Data
    public static class AllergyItem {
        private Long id;          // primary key row id
        private String fhirId;    // FHIR resource ID
        private String externalId; // Alias for fhirId
        
        @NotBlank(message = "Allergy name is required")
        private String allergyName;
        
        @NotBlank(message = "Reaction is required")
        private String reaction;
        
        @NotBlank(message = "Severity is required")
        private String severity;
        
        @NotBlank(message = "Status is required")
        private String status;
        
        private Long patientId;

        // Effective window
        private String startDate;  // ISO yyyy-MM-dd preferred
        private String endDate;    // ISO yyyy-MM-dd preferred

        // NEW
        private String comments;
        private Audit audit;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

package org.ciyex.ehr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MedicalProblemDto {

    private Long id;
    private String externalId;
    private String fhirId;
    private String tenantName;
    private Long patientId;
    
    @jakarta.validation.constraints.NotEmpty(message = "At least one medical problem is required")
    @jakarta.validation.Valid
    private List<MedicalProblemItem> problemsList;
    
    private Audit audit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class MedicalProblemItem {
        private Long id;
        private String externalId;
        private String fhirId;

        @jakarta.validation.constraints.NotBlank(message = "Title is required")
        private String title;
        
        @jakarta.validation.constraints.NotBlank(message = "Outcome is required")
        private String outcome;
        
        @jakarta.validation.constraints.NotBlank(message = "Verification status is required")
        private String verificationStatus;
        
        @jakarta.validation.constraints.NotBlank(message = "Occurrence is required")
        private String occurrence;
        
        private String note;

        private Long patientId;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Audit {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdDate;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastModifiedDate;
    }
}

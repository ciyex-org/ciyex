package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientEducationAssignmentDto {
    private Long id;
    
    @NotNull(message = "Patient ID is mandatory")
    private Long patientId;
    
    private String notes;
    private boolean delivered;
    private String assignedDate;
    private String patientName;
    private String fhirId;
    private String assignedBy;

    @NotNull(message = "Topic is mandatory")
    private TopicDto topic;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;

    @Data
    public static class TopicDto {
        private Long id;
        
        @NotNull(message = "Topic title is mandatory")
        private String title;
        
        private String summary;
        private String category;
        private String language;
        private String readingLevel;
        private String content;
        private String fhirId;
    }
}

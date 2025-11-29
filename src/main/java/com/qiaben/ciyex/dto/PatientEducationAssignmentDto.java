package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PatientEducationAssignmentDto {
    private Long id;
    private Long patientId;
    private String notes;
    private boolean delivered;
    private String assignedDate;
    private String patientName; // ✅ add this
    private String fhirId; // FHIR ID at assignment level

    private TopicDto topic; // ✅ Nested topic object

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;

    @Data
    public static class TopicDto {
        private Long id;
        private String title;
        private String summary;
        private String category;
        private String language;
        private String readingLevel;
        private String content;
        private String fhirId;
    }
}

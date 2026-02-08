package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class PatientEducationDto {
    private Long id;

    private String title;
    private String summary;
    private String category;
    private String language;
    private String readingLevel;
    private String content;

    private Audit audit;
    private String fhirId;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

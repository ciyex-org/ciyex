package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirDiagnosticReportDTO {
    private String id;
    private MetaDTO meta;
    private String resourceType;
    private String status;
    private CategoryDTO[] category;
    private CodeDTO code;
    private SubjectDTO subject;
    private EncounterDTO encounter;
    private String effectiveDateTime;
    private String issued;
    private PerformerDTO[] performer;
    private PresentedFormDTO[] presentedForm;

    @Data
    @NoArgsConstructor
    public static class MetaDTO {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class CategoryDTO {
        private CodingDTO[] coding;
    }

    @Data
    @NoArgsConstructor
    public static class CodeDTO {
        private CodingDTO[] coding;
    }

    @Data
    @NoArgsConstructor
    public static class CodingDTO {
        private String system;
        private String code;
        private String display;
    }

    @Data
    @NoArgsConstructor
    public static class SubjectDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class EncounterDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class PerformerDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class PresentedFormDTO {
        private String contentType;
        private String data;
    }
}


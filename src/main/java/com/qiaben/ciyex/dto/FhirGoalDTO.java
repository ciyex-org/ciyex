package com.qiaben.ciyex.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirGoalDTO {
    private String id;
    private MetaDTO meta;
    private String resourceType;
    private String lifecycleStatus;
    private DescriptionDTO description;
    private SubjectDTO subject;
    private TargetDTO[] target;

    @Data
    @NoArgsConstructor
    public static class MetaDTO {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class DescriptionDTO {
        private String text;
    }

    @Data
    @NoArgsConstructor
    public static class SubjectDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class TargetDTO {
        private MeasureDTO measure;
        private String detailString;
        private String dueDate;
    }

    @Data
    @NoArgsConstructor
    public static class MeasureDTO {
        private ExtensionDTO[] extension;
    }

    @Data
    @NoArgsConstructor
    public static class ExtensionDTO {
        private String valueCode;
        private String url;
    }
}


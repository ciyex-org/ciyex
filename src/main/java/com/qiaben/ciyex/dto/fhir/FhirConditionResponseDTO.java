package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirConditionResponseDTO {
    private String resourceType;
    private Meta meta;
    private String clinicalStatus;
    private String verificationStatus;
    private String category;
    private Code code;
    private Subject subject;

    @Data
    @NoArgsConstructor
    public static class Meta {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class Code {
        private Coding[] coding;
    }

    @Data
    @NoArgsConstructor
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }

    @Data
    @NoArgsConstructor
    public static class Subject {
        private String reference;
    }
}

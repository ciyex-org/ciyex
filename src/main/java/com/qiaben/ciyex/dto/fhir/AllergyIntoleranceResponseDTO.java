package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class AllergyIntoleranceResponseDTO {
    private String id;
    private Meta meta;
    private String resourceType;
    private Text text;
    private String clinicalStatus;
    private String verificationStatus;
    private String[] category;
    private String criticality;
    private Code code;
    private Patient patient;
    private Reaction[] reaction;

    @Data
    public static class Meta {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    public static class Text {
        private String status;
        private String div;
    }

    @Data
    public static class Code {
        private Coding[] coding;
    }

    @Data
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }

    @Data
    public static class Patient {
        private String reference;
    }

    @Data
    public static class Reaction {
        private Manifestation[] manifestation;
    }

    @Data
    public static class Manifestation {
        private Coding[] coding;
        private String text;
    }
}
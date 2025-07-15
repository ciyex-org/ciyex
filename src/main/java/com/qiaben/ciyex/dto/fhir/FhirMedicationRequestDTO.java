package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirMedicationRequestDTO {
    private String id;
    private MetaDTO meta;
    private String resourceType;
    private String status;
    private String intent;
    private String category;
    private Boolean reportedBoolean;
    private MedicationCodeableConceptDTO medicationCodeableConcept;
    private SubjectDTO subject;
    private String authoredOn;
    private RequesterDTO requester;

    @Data
    @NoArgsConstructor
    public static class MetaDTO {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class MedicationCodeableConceptDTO {
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
    public static class RequesterDTO {
        private String reference;
        private String type;
    }
}


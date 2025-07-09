package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImmunizationResponseDTO {
    private String id;
    private String resourceType;
    private LocalDateTime lastUpdated;
    private String status;
    private VaccineCode vaccineCode;
    private PatientReference patient;
    private LocalDateTime occurrenceDateTime;
    private LocalDateTime recorded;
    private boolean primarySource;

    @Data
    public static class VaccineCode {
        private Coding[] coding;

        @Data
        public static class Coding {
            private String system;
            private String code;
            private String display;
        }
    }

    @Data
    public static class PatientReference {
        private String reference;
    }
}

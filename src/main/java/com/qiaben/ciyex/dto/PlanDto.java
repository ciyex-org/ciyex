package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    // Business fields
    private String diagnosticPlan;
    private String plan;
    private String notes;
    private String followUpVisit;
    private String returnWorkSchool;

    // Accept real JSON in API
    private JsonNode sectionsJson;

    // Audit
    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

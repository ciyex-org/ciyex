//package com.qiaben.ciyex.dto;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.Data;
//
//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class PlanDto {
//    private Long id;
//    private String externalId;
//    private Long orgId;
//    private Long patientId;
//    private Long encounterId;
//
//    // Business fields
//    private String diagnosticPlan;
//    private String plan;
//    private String notes;
//    private String followUpVisit;
//    private String returnWorkSchool;
//
//    // Accept real JSON in API
//    private JsonNode sectionsJson;
//
//    // Audit
//    private Audit audit;
//    @Data
//    public static class Audit {
//        private String createdDate;
//        private String lastModifiedDate;
//    }
//}




package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PlanDto {
    private Long id;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    public String diagnosticPlan;
    public String plan;
    public String notes;
    public String followUpVisit;
    public String returnWorkSchool;
    public String sectionsJson; // PLAIN STRING

    public Boolean eSigned;
    public String  signedAt;    // ISO string
    public String  signedBy;
    public String  printedAt;   // ISO string

    public Audit audit;
    @Data public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

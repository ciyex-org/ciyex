//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class AssignedProviderDto {
//    private Long id;
//    private String externalId;      // optional FHIR id
//    private Long orgId;
//    private Long patientId;
//    private Long encounterId;
//
//    private Long providerId;        // FK to your Provider
//    private String role;            // PRIMARY | ATTENDING | REFERRING | CONSULTING | NURSE | OTHER
//    private String startDate;       // yyyy-MM-dd
//    private String endDate;         // yyyy-MM-dd (optional)
//    private String status;          // active | inactive | ended
//    private String notes;
//
//    private Audit audit;
//    @Data
//    public static class Audit {
//        private String createdDate;      // yyyy-MM-dd
//        private String lastModifiedDate; // yyyy-MM-dd
//    }
//}


package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AssignedProviderDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private Long providerId;
    private String role;
    private String startDate;
    private String endDate;
    private String status;
    private String notes;

    // eSign / Print (server-managed)
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}

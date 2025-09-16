//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class DateTimeFinalizedDto {
//    private Long id;
//    private String externalId;        // optional (FHIR id)
//    private Long orgId;
//    private Long patientId;
//    private Long encounterId;
//
//    // What is being finalized
//    private String targetType;        // NOTE | REPORT | PLAN | PROCEDURE | CLAIM | INVOICE | OTHER
//    private Long   targetId;          // local DB id of the target
//    private String targetVersion;     // optional version string
//
//    // Finalization details
//    private String finalizedAt;       // ISO 8601 e.g., 2025-08-20T10:05:00Z
//    private String finalizedBy;       // user/practitioner
//    private String finalizerRole;     // MD | RN | BILLING | ADMIN ...
//    private String method;            // AUTO | MANUAL | BULK
//    private String status;            // finalized | rolledback | amended
//    private String reason;            // short reason (optional)
//    private String comments;          // free text
//    private String contentHash;       // SHA-256 hex (optional)
//
//    // Optional linkage to signature/signoff if you keep ids
//    private Long providerSignatureId; // optional
//    private Long signoffId;           // optional
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
public class DateTimeFinalizedDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private String targetType;
    private Long targetId;
    private String targetVersion;

    private String finalizedAt;
    private String finalizedBy;
    private String finalizerRole;
    private String method;
    private String status;
    private String reason;
    private String comments;
    private String contentHash;
    private Long providerSignatureId;
    private Long signoffId;

    // eSign / Print (server-managed)
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    // compact audit for your UI card
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}

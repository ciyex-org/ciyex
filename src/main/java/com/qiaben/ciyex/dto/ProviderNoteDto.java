//package com.qiaben.ciyex.dto;
//
//import lombok.*;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ProviderNoteDto {
//    private String noteTitle;
//    private String noteTypeCode;
//    private String noteStatus;
//    private String noteDateTime;        // "2025-09-09" or "2025-09-09T12:00:00"
//    private String authorPractitionerId; // "94"
//    private String subjective;
//    private String objective;
//    private String assessment;
//    private String plan;
//    private String narrative;
//    private String externalId;
//
//    // ids (optional to return in responses)
//    private String id;
//    private String orgId;
//    private String patientId;
//    private String encounterId;
//    private String createdAt;
//    private String updatedAt;
//}






package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ProviderNoteDto {
    private Long id;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private String noteTitle;
    private String noteTypeCode;
    private String noteStatus;
    private String noteDateTime;         // ISO string is fine for UI
    private Long   authorPractitionerId;

    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    private String narrative;

    private String externalId;

    // server-managed eSign/print state
    private Boolean eSigned;
    private String  signedAt;            // ISO string
    private String  signedBy;
    private String  printedAt;           // ISO string

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}

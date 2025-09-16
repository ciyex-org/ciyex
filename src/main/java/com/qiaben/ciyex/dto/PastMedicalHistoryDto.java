//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class PastMedicalHistoryDto {
//    private Long id;             // DB id
//    private String externalId;   // FHIR id (optional)
//    private Long orgId;          // tenant
//    private Long patientId;
//    private Long encounterId;
//
//    private String description;  // PMH narrative
//
//    private Audit audit;
//
//    @Data
//    public static class Audit {
//        // Keep strings to align with yyyy-MM-dd preference
//        private String createdDate;
//        private String lastModifiedDate;
//    }
//}




package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PastMedicalHistoryDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private String description;

    // server-managed eSign/print fields
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

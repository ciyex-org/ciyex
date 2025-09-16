//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class FamilyHistoryDto {
//    private Long id;               // parent record id
//    private String externalId;     // FHIR id (optional)
//    private Long orgId;
//    private Long patientId;
//    private Long encounterId;
//
//    private List<EntryDto> entries;  // one row per relation/diagnosis
//
//    private Audit audit;
//
//    @Data
//    public static class EntryDto {
//        private Long id;                // child row id
//        private String relation;        // FATHER | MOTHER | SIBLING | SPOUSE | OFFSPRING
//        private String diagnosisCode;   // e.g., ICD-10
//        private String diagnosisText;   // optional free text
//        private String notes;           // optional
//    }
//
//    @Data
//    public static class Audit {
//        private String createdDate;      // yyyy-MM-dd
//        private String lastModifiedDate; // yyyy-MM-dd
//    }
//}


package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class FamilyHistoryDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    // container-level eSign
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;
    private Long signedEntryId;

    // entries (simple strings)
    private List<EntryDto> entries;

    // audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

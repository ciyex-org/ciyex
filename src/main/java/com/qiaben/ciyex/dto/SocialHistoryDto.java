//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class SocialHistoryDto {
//    private Long id;
//    private String externalId;   // optional FHIR id
//    private Long orgId;
//    private Long patientId;
//    private Long encounterId;
//
//    private List<EntryDto> entries; // smoking, alcohol, occupation, etc.
//
//    private Audit audit;
//
//    @Data
//    public static class EntryDto {
//        // SMOKING | ALCOHOL | DRUGS | OCCUPATION | MARITAL_STATUS | EXERCISE | DIET | HOUSING | EDUCATION | SEXUAL_HISTORY | OTHER
//        private String category;
//        private String value;      // e.g., "Current every day smoker", "Occasional", "Engineer"
//        private String details;    // free text notes
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

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class SocialHistoryDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private List<SocialHistoryEntryDto> entries;

    // server-managed
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}


//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class ReviewOfSystemDto {
//    private Long id;
//    private String externalId;
//
//       // tenant
//    private Long patientId;   // scope
//    private Long encounterId; // scope
//
//    // Single ROS line item per “system”
//    private String systemName;         // e.g., "Cardiovascular"
//    private Boolean isNegative;        // true = all negative, false = positive findings present
//    private String notes;              // free text for the system
//
//    private List<String> systemDetails; // e.g., ["Chest pain", "Shortness of breath"]
//
//    // client-friendly audit strings (mapped from created_at/updated_at)
//    private String createdDate;        // yyyy-MM-dd
//    private String lastModifiedDate;   // yyyy-MM-dd
//}

package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReviewOfSystemDto {
    private Long id;
    private Long patientId;
    private Long encounterId;

    private String systemName;      // e.g., "HEENT"
    private Boolean isNegative;     // true = Negative, false = Positive
    private String notes;
    private List<String> systemDetails; // findings list

    // server-managed eSign/print
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

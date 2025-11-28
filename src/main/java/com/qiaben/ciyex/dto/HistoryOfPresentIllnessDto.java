////package com.qiaben.ciyex.dto;
////
////import java.time.LocalDateTime;
////
////public class HistoryOfPresentIllnessDto {
////
////    private Long id;
////    private String description;  // Description of the illness
////    private Long patientId;      // Patient ID
////    private Long encounterId;    // Encounter ID
////          // Organization ID
////    private LocalDateTime createdAt;
////    private LocalDateTime updatedAt;
////
////    // Getters and Setters
////    public Long getId() {
////        return id;
////    }
////
////    public void setId(Long id) {
////        this.id = id;
////    }
////
////    public String getDescription() {
////        return description;
////    }
////
////    public void setDescription(String description) {
////        this.description = description;
////    }
////
////    public Long getPatientId() {
////        return patientId;
////    }
////
////    public void setPatientId(Long patientId) {
////        this.patientId = patientId;
////    }
////
////    public Long getEncounterId() {
////        return encounterId;
////    }
////
////    public void setEncounterId(Long encounterId) {
////        this.encounterId = encounterId;
////    }
////
////    
////
////    
////
////    public LocalDateTime getCreatedAt() {
////        return createdAt;
////    }
////
////    public void setCreatedAt(LocalDateTime createdAt) {
////        this.createdAt = createdAt;
////    }
////
////    public LocalDateTime getUpdatedAt() {
////        return updatedAt;
////    }
////
////    public void setUpdatedAt(LocalDateTime updatedAt) {
////        this.updatedAt = updatedAt;
////    }
////}
//
//
//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class HistoryOfPresentIllnessDto {
//    private Long id;             // DB id
//    private String externalId;   // FHIR id (optional)
//          // tenant
//    private Long patientId;
//    private Long encounterId;
//
//    private String description;  // HPI narrative
//
//    private Audit audit;
//
//    @Data
//    public static class Audit {
//        // keep string dates to match your yyyy-MM-dd preference
//        private String createdDate;
//        private String lastModifiedDate;
//    }
//}

package com.qiaben.ciyex.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class HistoryOfPresentIllnessDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    private String description; // HPI narrative

    // server-managed eSign/print
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


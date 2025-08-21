//package com.qiaben.ciyex.dto;
//
//import java.time.LocalDateTime;
//
//public class HistoryOfPresentIllnessDto {
//
//    private Long id;
//    private String description;  // Description of the illness
//    private Long patientId;      // Patient ID
//    private Long encounterId;    // Encounter ID
//    private Long orgId;          // Organization ID
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//    // Getters and Setters
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    public Long getPatientId() {
//        return patientId;
//    }
//
//    public void setPatientId(Long patientId) {
//        this.patientId = patientId;
//    }
//
//    public Long getEncounterId() {
//        return encounterId;
//    }
//
//    public void setEncounterId(Long encounterId) {
//        this.encounterId = encounterId;
//    }
//
//    public Long getOrgId() {
//        return orgId;
//    }
//
//    public void setOrgId(Long orgId) {
//        this.orgId = orgId;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public LocalDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(LocalDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//}


package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class HistoryOfPresentIllnessDto {
    private Long id;             // DB id
    private String externalId;   // FHIR id (optional)
    private Long orgId;          // tenant
    private Long patientId;
    private Long encounterId;

    private String description;  // HPI narrative

    private Audit audit;

    @Data
    public static class Audit {
        // keep string dates to match your yyyy-MM-dd preference
        private String createdDate;
        private String lastModifiedDate;
    }
}

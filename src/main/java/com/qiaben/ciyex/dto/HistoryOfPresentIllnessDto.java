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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * History of Present Illness (HPI) DTO
 * Date strings use yyyy-MM-dd to match project conventions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryOfPresentIllnessDto {
    private Long id;                 // DB ID
    private String externalId;       // External FHIR ID
    private Long orgId;              // Tenant
    private Long patientId;          // Internal patient id
    private Long encounterId;        // Internal encounter id

    // HPI content
    private String hpiText;          // Narrative text
    private String onsetDate;        // yyyy-MM-dd (optional)
    private String duration;         // e.g., "2 weeks"
    private String severity;         // e.g., "mild", "moderate", "severe"
    private String associatedSymptoms;// free text
    private String modifyingFactors; // free text
    private String notes;            // additional notes

    // Audit
    private String createdDate;      // yyyy-MM-dd
    private String lastModifiedDate; // yyyy-MM-dd
}

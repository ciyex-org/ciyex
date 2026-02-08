////package org.ciyex.ehr.dto;
////
////import java.time.LocalDateTime;
////
////public class ChiefComplaintDto {
////
////    private Long id;
////    private String complaint;
////    private String details;
////    private Long encounterId;
//// // Organization ID passed in header
////    private LocalDateTime createdAt;
////    private LocalDateTime updatedAt;
////    private Long patientId;
////
////    public Long getPatientId() {
////        return patientId;
////    }
////
////    public void setPatientId(Long patientId) {
////        this.patientId = patientId;
////    }
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
////    public String getComplaint() {
////        return complaint;
////    }
////
////    public void setComplaint(String complaint) {
////        this.complaint = complaint;
////    }
////
////    public String getDetails() {
////        return details;
////    }
////
////    public void setDetails(String details) {
////        this.details = details;
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
//package org.ciyex.ehr.dto;
//
//import java.time.LocalDateTime;
//
//public class ChiefComplaintDto {
//
//    private Long id;
//    private String complaint;
//    private String details;
//    private Long encounterId;
// // Organization ID passed in header
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private Long patientId;
//
//    // Getters and Setters
//    public Long getPatientId() {
//        return patientId;
//    }
//
//    public void setPatientId(Long patientId) {
//        this.patientId = patientId;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getComplaint() {
//        return complaint;
//    }
//
//    public void setComplaint(String complaint) {
//        this.complaint = complaint;
//    }
//
//    public String getDetails() {
//        return details;
//    }
//
//    public void setDetails(String details) {
//        this.details = details;
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
//    
//
//    
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
//
//

package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class ChiefComplaintDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    private String complaint;
    private String details;
    private String severity;
    private String status;

    // eSign/Print
    private Boolean eSigned;
    private String  signedAt;   // ISO string
    private String  signedBy;
    private String  printedAt;  // ISO string

    // audit
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}

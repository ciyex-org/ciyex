////package com.qiaben.ciyex.entity;
////
////
////import jakarta.persistence.Entity;
////import jakarta.persistence.GeneratedValue;
////import jakarta.persistence.GenerationType;
////import jakarta.persistence.Id;
////
////import java.time.LocalDateTime;
////
////@Entity
//////@Table(name = "chief_complaints")
////public class ChiefComplaint {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    private Long id;
////
////    private String complaint;
////    private String details;
////    private Long encounterId;
////    private Long orgId; // Organization ID (passed in header)
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
////    public Long getOrgId() {
////        return orgId;
////    }
////
////    public void setOrgId(Long orgId) {
////        this.orgId = orgId;
////    }
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
//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//
//import java.time.LocalDateTime;
//
//@Entity
//public class ChiefComplaint {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String complaint;
//    private String details;
//    private Long encounterId;
//    private Long orgId;
//    private Long patientId;
//
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
//
//    public Long getPatientId() {
//        return patientId;
//    }
//
//    public void setPatientId(Long patientId) {
//        this.patientId = patientId;
//    }
//}
//
//



package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "chief_complaint")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChiefComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint", length = 255)
    private String complaint;

    @Column(name = "details", length = 255)
    private String details;

    @Column(name = "severity", length = 255)
    private String severity;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    // eSign / Print
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // audit
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

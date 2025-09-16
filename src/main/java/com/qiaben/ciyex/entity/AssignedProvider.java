//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "assigned_providers")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class AssignedProvider {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "external_id")
//    private String externalId;
//
//    @Column(name = "org_id", nullable = false)
//    private Long orgId;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "provider_id", nullable = false)
//    private Long providerId;
//
//    @Column(name = "role", length = 32, nullable = false)
//    private String role;                 // PRIMARY, ATTENDING, etc.
//
//    @Column(name = "start_date", length = 16)
//    private String startDate;            // yyyy-MM-dd
//
//    @Column(name = "end_date", length = 16)
//    private String endDate;              // yyyy-MM-dd
//
//    @Column(name = "status", length = 24)
//    private String status;               // active | inactive | ended
//
//    @Column(name = "notes", columnDefinition = "TEXT")
//    private String notes;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//}


package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assigned_providers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AssignedProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "role", length = 32, nullable = false)
    private String role;

    @Column(name = "start_date", length = 16)
    private String startDate; // keep as String per your schema

    @Column(name = "end_date", length = 16)
    private String endDate;   // keep as String per your schema

    @Column(name = "status", length = 24)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- eSign / Print ----
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // ---- audit ----
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

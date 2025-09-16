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
//@Table(name = "date_time_finalized")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class DateTimeFinalized {
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
//    @Column(name = "target_type", length = 32)
//    private String targetType;
//
//    @Column(name = "target_id")
//    private Long targetId;
//
//    @Column(name = "target_version", length = 64)
//    private String targetVersion;
//
//    @Column(name = "finalized_at", length = 40, nullable = false)
//    private String finalizedAt;
//
//    @Column(name = "finalized_by", length = 128)
//    private String finalizedBy;
//
//    @Column(name = "finalizer_role", length = 64)
//    private String finalizerRole;
//
//    @Column(name = "method", length = 16)
//    private String method;
//
//    @Column(name = "status", length = 24)
//    private String status;
//
//    @Column(name = "reason", length = 256)
//    private String reason;
//
//    @Column(name = "comments", columnDefinition = "TEXT")
//    private String comments;
//
//    @Column(name = "content_hash", length = 128)
//    private String contentHash;
//
//    @Column(name = "provider_signature_id")
//    private Long providerSignatureId;
//
//    @Column(name = "signoff_id")
//    private Long signoffId;
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
@Table(name = "date_time_finalized")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DateTimeFinalized {

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

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_version", length = 64)
    private String targetVersion;

    // keep as String to match your schema/UI
    @Column(name = "finalized_at", length = 40, nullable = false)
    private String finalizedAt;

    @Column(name = "finalized_by", length = 128)
    private String finalizedBy;

    @Column(name = "finalizer_role", length = 64)
    private String finalizerRole;

    @Column(name = "method", length = 16)
    private String method;

    @Column(name = "status", length = 24)
    private String status;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "provider_signature_id")
    private Long providerSignatureId;

    @Column(name = "signoff_id")
    private Long signoffId;

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

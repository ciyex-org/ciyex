//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.annotations.UpdateTimestamp;
//import org.hibernate.type.SqlTypes;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "signoff")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class Signoff {
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
//    @Column(name = "status", length = 24)
//    private String status;
//
//    @Column(name = "signed_by", length = 128)
//    private String signedBy;
//
//    @Column(name = "signer_role", length = 64)
//    private String signerRole;
//
//    @Column(name = "signed_at", length = 40)
//    private String signedAt;
//    @Column(name = "content_hash", length = 128)
//    private String contentHash;
//
//    @Column(name = "signature_type", length = 32)
//    private String signatureType;
//
//
//    @Column(name = "signature_data", columnDefinition = "text")
//    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
//    private String signatureData;
//
//    @Column(name = "attestation_text", columnDefinition = "text")
//    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
//    private String attestationText;
//
//    @Column(name = "comments", columnDefinition = "text")
//    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
//    private String comments;
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

@Entity
@Table(name = "signoff")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Signoff {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "status", length = 24)
    private String status; // Draft, Signed, Locked, ...

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "signer_role", length = 64)
    private String signerRole;

    // varchar per schema → keep as String
    @Column(name = "signed_at", length = 40)
    private String signedAt;

    @Column(name = "signature_type", length = 32)
    private String signatureType;

    @Column(name = "signature_data", columnDefinition = "text")
    private String signatureData;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "attestation_text", columnDefinition = "text")
    private String attestationText;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    // optional if you added the column
    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

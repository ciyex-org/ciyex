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
//@Table(name = "provider_signatures")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class ProviderSignature {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "external_id")
//    private String externalId;
//
//    
//    private Long orgId;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "signed_at", length = 40, nullable = false)
//    private String signedAt;
//
//    @Column(name = "signed_by", length = 128)
//    private String signedBy;
//
//    @Column(name = "signer_role", length = 64)
//    private String signerRole;
//
//    @Column(name = "signature_type", length = 32)
//    private String signatureType;
//
//    @Column(name = "signature_format", length = 64)
//    private String signatureFormat;
//
//    @Column(name = "signature_data", columnDefinition = "TEXT")
//    private String signatureData;          // don’t use @Lob; TEXT is fine in Postgres
//
//    @Column(name = "signature_hash", length = 128)
//    private String signatureHash;
//
//    @Column(name = "status", length = 64)
//    private String status;
//
//    @Column(name = "comments", columnDefinition = "TEXT")
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
@Table(name = "provider_signatures")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    // Stored as varchar(40) per schema
    @Column(name = "signed_at", nullable = false, length = 40)
    private String signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "signer_role", length = 64)
    private String signerRole;

    @Column(name = "signature_type", length = 32)
    private String signatureType;

    @Column(name = "signature_format", length = 64)
    private String signatureFormat;

    @Column(name = "signature_data", columnDefinition = "text")
    private String signatureData; // base64

    @Column(name = "signature_hash", length = 128)
    private String signatureHash;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

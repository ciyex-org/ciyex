//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(
//        name = "codes",
//        indexes = {
//                @Index(name = "idx_codes_scope", columnList = "org_id, patient_id, encounter_id"),
//                @Index(name = "idx_codes_type_code", columnList = "code_type, code")
//        }
//)
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class Code {
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
//    @Column(name = "code_type", length = 16, nullable = false)
//    private String codeType; // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM
//
//    @Column(name = "code", length = 32, nullable = false)
//    private String code;
//
//    @Column(name = "modifier", length = 16)
//    private String modifier;
//
//    @Column(name = "active")
//    private Boolean active;
//
//    @Column(name = "description", columnDefinition = "TEXT")
//    private String description;
//
//    @Column(name = "short_description", length = 256)
//    private String shortDescription;
//
//    @Column(name = "category", length = 64)
//    private String category;
//
//    @Column(name = "diagnosis_reporting")
//    private Boolean diagnosisReporting;
//
//    @Column(name = "service_reporting")
//    private Boolean serviceReporting;
//
//    @Column(name = "relate_to", length = 128)
//    private String relateTo;
//
//    @Column(name = "fee_standard", precision = 18, scale = 2)
//    private BigDecimal feeStandard;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "codes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Code {

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

    @Column(name = "code_type", nullable = false, length = 16)
    private String codeType;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "modifier", length = 16)
    private String modifier;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "short_description", length = 256)
    private String shortDescription;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "diagnosis_reporting")
    private Boolean diagnosisReporting;

    @Column(name = "service_reporting")
    private Boolean serviceReporting;

    @Column(name = "relate_to", length = 128)
    private String relateTo;

    @Column(name = "fee_standard", precision = 18, scale = 2)
    private BigDecimal feeStandard;

    // --- eSign / Print ---
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
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

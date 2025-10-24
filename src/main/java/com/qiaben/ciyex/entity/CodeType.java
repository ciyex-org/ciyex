package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_types",
        indexes = {
                @Index(name = "idx_codetypes_scope", columnList = "org_id, patient_id, encounter_id"),
                @Index(name = "idx_codetypes_key", columnList = "code_type_key, code_type_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "code_type_key", length = 64, nullable = false)
    private String codeTypeKey;

    @Column(name = "code_type_id")
    private Integer codeTypeId;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "modifier")
    private Integer modifier;

    @Column(name = "justification", length = 128)
    private String justification;

    @Column(name = "mask", length = 128)
    private String mask;

    @Column(name = "fee_applicable")
    private Boolean feeApplicable;

    @Column(name = "related_indicator")
    private Boolean relatedIndicator;

    @Column(name = "number_of_services")
    private Boolean numberOfServices;

    @Column(name = "diagnosis_flag")
    private Boolean diagnosisFlag;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "label", length = 256)
    private String label;

    @Column(name = "external_flag")
    private Boolean externalFlag;

    @Column(name = "claim_flag")
    private Boolean claimFlag;

    @Column(name = "procedure_flag")
    private Boolean procedureFlag;

    @Column(name = "terminology_flag")
    private Boolean terminologyFlag;

    @Column(name = "problem_flag")
    private Boolean problemFlag;

    @Column(name = "drug_flag")
    private Boolean drugFlag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

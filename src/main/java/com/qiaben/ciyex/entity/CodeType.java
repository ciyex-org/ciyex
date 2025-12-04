package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_types",
        indexes = {
                @Index(name = "idx_codetypes_key", columnList = "code_type_key, code_type_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class CodeType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "fhir_id")
    private String fhirId;

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

        // audit fields provided by AuditableEntity

        // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
        public LocalDateTime getCreatedAt() { return getCreatedDate(); }
        public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
        public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
        public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "codess",
        indexes = {
                @Index(name = "idx_codes_type_code", columnList = "code_type, code")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class GlobalCode extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    

    @Column(name = "code_type", length = 16, nullable = false)
    private String codeType; // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM

    @Column(name = "code", length = 32, nullable = false)
    private String code;

    @Column(name = "modifier", length = 16)
    private String modifier;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description", columnDefinition = "TEXT")
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

    // audit fields provided by AuditableEntity

    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}

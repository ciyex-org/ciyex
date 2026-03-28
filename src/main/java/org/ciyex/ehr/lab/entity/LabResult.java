package org.ciyex.ehr.lab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "lab_result")
@Builder @NoArgsConstructor @AllArgsConstructor
public class LabResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id")
    private LabOrder labOrder;

    private Long patientId;
    private Long encounterId;
    private String orderNumber;
    private String procedureName;
    private String testCode;
    private String testName;
    private String loincCode;
    private String status; // Pending, Preliminary, Final, Corrected, Amended
    private String specimen;
    private LocalDate collectedDate;
    private LocalDate reportedDate;
    private String abnormalFlag; // Normal, Low, High, Critical, Abnormal
    private String value;
    private BigDecimal numericValue;
    private String units;
    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private String referenceRange;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(columnDefinition = "TEXT")
    private String recommendations;
    private Boolean signed;
    private LocalDateTime signedAt;
    private String signedBy;
    private String panelName;
    private String panelCode;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

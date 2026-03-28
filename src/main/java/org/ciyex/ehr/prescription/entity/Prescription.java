package org.ciyex.ehr.prescription.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "prescription")
@Builder @NoArgsConstructor @AllArgsConstructor
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String prescriberName;
    private String prescriberNpi;
    @Column(nullable = false)
    private String medicationName;
    private String medicationCode;
    private String medicationSystem;
    private String strength;
    private String dosageForm;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sig;
    private Integer quantity;
    private String quantityUnit;
    private Integer daysSupply;
    @Builder.Default
    private Integer refills = 0;
    @Builder.Default
    private Integer refillsRemaining = 0;
    private String pharmacyName;
    private String pharmacyPhone;
    @Column(columnDefinition = "TEXT")
    private String pharmacyAddress;
    private String status;
    private String priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate discontinuedDate;
    @Column(columnDefinition = "TEXT")
    private String discontinuedReason;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String deaSchedule;
    @Column(nullable = false)
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

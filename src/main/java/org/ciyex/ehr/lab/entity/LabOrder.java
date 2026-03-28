package org.ciyex.ehr.lab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "lab_order")
@Builder @NoArgsConstructor @AllArgsConstructor
public class LabOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String orderNumber;
    private String orderName;
    private String testCode;
    private String testDisplay;
    private String status; // draft, active, pending, completed, cancelled, revoked
    private String priority; // routine, urgent, stat
    private LocalDate orderDate;
    private LocalDateTime orderDateTime;
    private String labName;
    private String orderingProvider;
    private String physicianName;
    private String specimenId;
    @Column(columnDefinition = "TEXT")
    private String diagnosisCode;
    @Column(columnDefinition = "TEXT")
    private String procedureCode;
    private String resultStatus; // Pending, Partial, Final
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

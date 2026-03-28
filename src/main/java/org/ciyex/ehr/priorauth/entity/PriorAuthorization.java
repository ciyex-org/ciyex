package org.ciyex.ehr.priorauth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "prior_authorization")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PriorAuthorization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    private String patientName;
    private String providerName;
    private String insuranceName;
    private String insuranceId;
    private String memberId;
    private String authNumber;
    private String procedureCode;
    private String procedureDescription;
    private String diagnosisCode;
    private String diagnosisDescription;

    @Column(nullable = false)
    private String status; // pending, submitted, approved, denied, appeal, expired, cancelled

    @Column(nullable = false)
    private String priority; // routine, urgent, stat

    @Column(nullable = false)
    private LocalDate requestedDate;
    private LocalDate reviewDate;
    private LocalDate approvedDate;
    private LocalDate deniedDate;
    private LocalDate expiryDate;
    private Integer approvedUnits;
    @Column(columnDefinition = "integer default 0")
    private Integer usedUnits;
    private Integer remainingUnits;
    @Column(columnDefinition = "TEXT")
    private String denialReason;
    private LocalDate appealDeadline;
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "org_alias", nullable = false)
    private String orgAlias;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

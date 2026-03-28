package org.ciyex.ehr.referral.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "referral")
@Builder @NoArgsConstructor @AllArgsConstructor
public class Referral {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    private String patientName;
    private String referringProvider;
    private String specialistName;
    private String specialistNpi;
    private String specialty;
    private String facilityName;
    private String facilityAddress;
    private String facilityPhone;
    private String facilityFax;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    @Column(columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(nullable = false)
    private String urgency; // routine, urgent, stat

    @Column(nullable = false)
    private String status; // draft, sent, acknowledged, scheduled, completed, cancelled, denied

    @Column(nullable = false)
    private LocalDate referralDate;
    private LocalDate expiryDate;
    private String authorizationNumber;
    private String insuranceName;
    private String insuranceId;
    private LocalDate appointmentDate;
    @Column(columnDefinition = "TEXT")
    private String appointmentNotes;
    @Column(columnDefinition = "TEXT")
    private String followUpNotes;

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

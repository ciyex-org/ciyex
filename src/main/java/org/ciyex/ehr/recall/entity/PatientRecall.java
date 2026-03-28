package org.ciyex.ehr.recall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_recall")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientRecall {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_alias", nullable = false)
    private String orgAlias;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @Column(name = "patient_phone", length = 30)
    private String patientPhone;

    @Column(name = "patient_email", length = 200)
    private String patientEmail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recall_type_id")
    private RecallType recallType;

    @Column(name = "recall_type_name", length = 100)
    private String recallTypeName;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "provider_name", length = 200)
    private String providerName;

    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, CONTACTED, SCHEDULED, COMPLETED, CANCELLED, OVERDUE

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "notification_date")
    private LocalDate notificationDate;

    @Column(name = "source_encounter_id", length = 100)
    private String sourceEncounterId;

    @Column(name = "source_appointment_id")
    private Long sourceAppointmentId;

    @Column(name = "linked_appointment_id")
    private Long linkedAppointmentId;

    @Column(name = "completed_encounter_id", length = 100)
    private String completedEncounterId;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_attempt_date")
    private LocalDateTime lastAttemptDate;

    @Column(name = "last_attempt_method", length = 20)
    private String lastAttemptMethod;

    @Column(name = "last_attempt_outcome", length = 30)
    private String lastAttemptOutcome;

    @Column(name = "next_attempt_date")
    private LocalDate nextAttemptDate;

    @Column(name = "preferred_contact", length = 20)
    private String preferredContact; // PHONE, SMS, EMAIL, PORTAL, LETTER

    @Column(nullable = false, length = 10)
    private String priority; // NORMAL, HIGH, LOW, URGENT

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "auto_created", nullable = false)
    private Boolean autoCreated;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (attemptCount == null) attemptCount = 0;
        if (autoCreated == null) autoCreated = false;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

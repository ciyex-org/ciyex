package org.ciyex.ehr.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "clinical_task")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClinicalTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", nullable = false)
    private String taskType; // general, follow_up, callback, refill, lab_review, referral, prior_auth, documentation

    @Column(nullable = false)
    private String status; // pending, in_progress, completed, cancelled, deferred

    @Column(nullable = false)
    private String priority; // low, normal, high, urgent

    private LocalDate dueDate;
    private LocalTime dueTime;
    private String assignedTo;
    private String assignedBy;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime completedAt;
    private String completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "org_alias", nullable = false)
    private String orgAlias;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

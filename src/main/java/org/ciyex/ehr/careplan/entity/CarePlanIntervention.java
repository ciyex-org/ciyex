package org.ciyex.ehr.careplan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "care_plan_intervention")
@Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanIntervention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CarePlan carePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CarePlanGoal goal; // nullable - intervention may not be tied to a specific goal

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    private String assignedTo;
    private String frequency; // daily, weekly, monthly, as_needed, once
    private String status; // active, completed, cancelled
    @Column(columnDefinition = "TEXT")
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

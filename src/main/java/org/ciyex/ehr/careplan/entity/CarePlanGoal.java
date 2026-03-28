package org.ciyex.ehr.careplan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data @Entity @Table(name = "care_plan_goal")
@Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CarePlan carePlan;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    private LocalDate targetDate;
    private String status; // in_progress, achieved, not_achieved, cancelled
    private String measure; // e.g., "HbA1c < 7%", "BMI < 25"
    private String currentValue;
    private String targetValue;
    private String priority; // low, medium, high
    @Column(columnDefinition = "TEXT")
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CarePlanIntervention> interventions = new ArrayList<>();

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

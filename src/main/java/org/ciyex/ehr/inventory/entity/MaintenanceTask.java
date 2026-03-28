package org.ciyex.ehr.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "maintenance_task")
@FilterDef(name = "orgAliasFilter", parameters = @ParamDef(name = "orgAlias", type = String.class))
@Filter(name = "orgAliasFilter", condition = "org_alias = :orgAlias")
public class MaintenanceTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_name", nullable = false, length = 200)
    private String equipmentName;

    @Column(name = "equipment_id", length = 100)
    private String equipmentId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String category = "preventive";

    @Column(length = 200)
    private String location;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(length = 200)
    private String assignee;

    @Column(length = 200)
    private String vendor;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "medium";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "scheduled";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "org_alias", nullable = false, length = 100)
    private String orgAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

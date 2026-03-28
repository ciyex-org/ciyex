package org.ciyex.ehr.cds.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "cds_rule")
@Builder @NoArgsConstructor @AllArgsConstructor
public class CdsRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String ruleType;       // preventive_screening, drug_allergy, drug_drug, duplicate_order, age_based, condition_based, lab_value, custom
    private String category;       // preventive, medication_safety, order_entry, chronic_disease
    private String triggerEvent;   // encounter_open, order_entry, medication_prescribe, lab_result, manual
    @Column(columnDefinition = "jsonb")
    private String conditions;     // {age_min, age_max, gender, diagnosis_codes[], medication_codes[], lab_codes[]}
    private String actionType;     // alert, reminder, suggestion, hard_stop
    private String severity;       // info, warning, critical
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(columnDefinition = "TEXT")
    private String recommendation;
    private String referenceUrl;
    private Boolean isActive;
    private String appliesTo;      // all, provider, nurse, ma
    private Integer snoozeDays;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

package org.ciyex.ehr.recall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recall_type")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_alias", nullable = false)
    private String orgAlias;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 30)
    private String category; // PREVENTIVE, DENTAL, CHRONIC, IMMUNIZATION, SCREENING, LAB, POST_PROCEDURE, SPECIALIST

    @Column(name = "interval_months", nullable = false)
    private Integer intervalMonths;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(nullable = false, length = 10)
    private String priority; // NORMAL, HIGH, LOW, URGENT

    @Column(name = "auto_create", nullable = false)
    private Boolean autoCreate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "communication_sequence", columnDefinition = "jsonb")
    private List<String> communicationSequence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_wait_days", columnDefinition = "jsonb")
    private List<Integer> escalationWaitDays;

    @Column(name = "appointment_type_code", length = 50)
    private String appointmentTypeCode;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

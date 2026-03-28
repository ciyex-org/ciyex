package org.ciyex.ehr.cds.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "cds_alert_log")
@Builder @NoArgsConstructor @AllArgsConstructor
public class CdsAlertLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ruleId;
    private String ruleName;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String alertType;
    private String severity;
    @Column(columnDefinition = "TEXT")
    private String message;
    private String actionTaken;    // acknowledged, overridden, acted_on, snoozed, dismissed
    @Column(columnDefinition = "TEXT")
    private String overrideReason;
    private String actedBy;
    private LocalDateTime actedAt;
    private String orgAlias;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}

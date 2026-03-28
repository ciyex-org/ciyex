package org.ciyex.ehr.recall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recall_outreach_log")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallOutreachLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recall_id", nullable = false)
    private PatientRecall recall;

    @Column(name = "org_alias", nullable = false)
    private String orgAlias;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "attempt_date", nullable = false)
    private LocalDateTime attemptDate;

    @Column(nullable = false, length = 20)
    private String method; // PHONE, SMS, EMAIL, PORTAL, LETTER

    @Column(nullable = false, length = 10)
    private String direction; // OUTBOUND, INBOUND

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "performed_by_name", length = 200)
    private String performedByName;

    @Column(nullable = false, length = 30)
    private String outcome; // REACHED, LEFT_VOICEMAIL, NO_ANSWER, WRONG_NUMBER, SCHEDULED, DECLINED, SENT, DELIVERED, BOUNCED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "next_action", length = 20)
    private String nextAction;

    @Column(name = "next_action_date")
    private LocalDate nextActionDate;

    @Column(nullable = false)
    private Boolean automated;

    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (attemptDate == null) attemptDate = LocalDateTime.now();
        if (automated == null) automated = false;
    }
}

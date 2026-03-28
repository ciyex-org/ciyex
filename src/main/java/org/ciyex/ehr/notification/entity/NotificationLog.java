package org.ciyex.ehr.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "notification_log")
@Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String channelType;
    private String recipient;
    private String recipientName;
    private String templateKey;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String body;
    private String status;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private String externalId;
    private Long patientId;
    private String patientName;
    private String sentBy;
    private String triggerType;
    @Column(columnDefinition = "jsonb")
    private String metadata;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String orgAlias;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}

package org.ciyex.ehr.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "notification_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_alias", "event_type"}))
@Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventType;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private String timing;
    private Long templateId;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

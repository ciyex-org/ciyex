package org.ciyex.ehr.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "notification_template")
@Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String templateKey;
    private String channelType;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Column(columnDefinition = "TEXT")
    private String htmlBody;
    private Boolean isActive;
    private Boolean isDefault;
    @Column(columnDefinition = "jsonb")
    private String variables;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

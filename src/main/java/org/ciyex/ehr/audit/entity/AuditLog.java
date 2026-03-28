package org.ciyex.ehr.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "audit_log")
@Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String action;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String userId;
    private String userName;
    private String userRole;
    private String ipAddress;
    @Column(columnDefinition = "jsonb")
    private String details;
    private Long patientId;
    private String patientName;
    private String orgAlias;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}

package org.ciyex.ehr.portal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "portal_access_request")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PortalAccessRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String status; // pending, approved, denied
    private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(columnDefinition = "TEXT")
    private String deniedReason;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

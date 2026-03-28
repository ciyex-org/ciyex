package org.ciyex.ehr.usermgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "feature_request")
@Builder @NoArgsConstructor @AllArgsConstructor
public class FeatureRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String category;

    @Column(length = 500, nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(length = 30)
    private String status;

    @Column(name = "org_alias", nullable = false, length = 100)
    private String orgAlias;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); if (status == null) status = "submitted"; }
}

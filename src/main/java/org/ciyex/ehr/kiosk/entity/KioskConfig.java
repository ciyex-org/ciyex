package org.ciyex.ehr.kiosk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "kiosk_config")
@Builder @NoArgsConstructor @AllArgsConstructor
public class KioskConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean enabled;
    @Column(columnDefinition = "jsonb")
    private String config;          // {verify_dob, verify_phone, update_demographics, update_insurance, sign_consent, collect_copay, show_wait_time}
    @Column(columnDefinition = "TEXT")
    private String welcomeMessage;
    @Column(columnDefinition = "TEXT")
    private String completionMessage;
    private Integer idleTimeoutSec;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}

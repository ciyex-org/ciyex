package org.ciyex.ehr.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_launch_logs")
public class AppLaunchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "app_installation_id")
    private UUID appInstallationId;

    @Column(name = "app_slug", nullable = false)
    private String appSlug;

    @Column(name = "launched_by", nullable = false)
    private String launchedBy;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "launch_type", nullable = false)
    private String launchType;

    @Column(name = "launched_at", nullable = false, updatable = false)
    private LocalDateTime launchedAt;

    @PrePersist
    void prePersist() {
        launchedAt = LocalDateTime.now();
    }
}

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
@Table(name = "app_usage_events")
public class AppUsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "app_slug", nullable = false)
    private String appSlug;

    @Column(name = "app_installation_id")
    private UUID appInstallationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_detail")
    private String eventDetail;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Builder.Default
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private Boolean reported = false;
}

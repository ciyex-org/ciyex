package org.ciyex.ehr.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_usage_daily")
public class AppUsageDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "app_slug", nullable = false)
    private String appSlug;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Builder.Default
    @Column(name = "total_count", nullable = false)
    private Long totalCount = 0L;

    @Builder.Default
    @Column(name = "unique_users", nullable = false)
    private Integer uniqueUsers = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reported = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

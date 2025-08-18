package com.qiaben.ciyex.entity;

import com.qiaben.ciyex.dto.ScheduleDto;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "provider_schedule")
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenancy
    private Long orgId;                   // scope like Provider.orgId  :contentReference[oaicite:0]{index=0}L14-L18

    // Links
    private Long providerId;              // FK (soft) to Provider.id

    // Core
    private String title;
    private String location;
    private String startDate;             // ISO date as string for simplicity
    private String endDate;               // nullable
    private String startTime;             // HH:mm
    private Integer durationMin;
    private Integer maxOccurrences;

    // Recurrence
    @Enumerated(EnumType.STRING)
    private ScheduleDto.Frequency frequency;

    private Integer intervalVal;          // “interval” is keyword in some DBs, so intervalVal
    private String weeklyDays;            // CSV of day indexes

    // Audit / status
    private String createdDate;
    private String lastModifiedDate;

    @Enumerated(EnumType.STRING)
    private ScheduleDto.ScheduleStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) status = ScheduleDto.ScheduleStatus.ACTIVE; // default ACTIVE
    }
}

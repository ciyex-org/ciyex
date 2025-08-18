package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ScheduleDto {
    private Long id;
    private Long orgId;
    private Long providerId;

    // Core
    private String title;          // e.g., “Clinic A — Morning”
    private String location;       // free text (replaces timezone)
    private String startDate;      // ISO date (YYYY-MM-DD)
    private String endDate;        // ISO date (nullable)
    private String startTime;      // HH:mm (24h)
    private Integer durationMin;   // minutes
    private Integer maxOccurrences;

    // Recurrence
    private Frequency frequency;   // DAILY, WEEKLY, MONTHLY
    private Integer interval;      // every N days/weeks/months
    private String weeklyDays;     // CSV of 0..6 (Sun..Sat), e.g. "1,2,3,4,5" for Mon–Fri

    // Status
    private ScheduleStatus status; // ACTIVE, ARCHIVED

    public enum Frequency { DAILY, WEEKLY, MONTHLY }
    public enum ScheduleStatus { ACTIVE, ARCHIVED }
}

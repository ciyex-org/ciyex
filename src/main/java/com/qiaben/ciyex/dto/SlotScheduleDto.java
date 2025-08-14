package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SlotScheduleDto {
    private Long providerId;
    private Long locationId;
    private LocalDate startDate; // Start date for generating slots
    private List<DaySchedule> schedule; // List of day-specific schedules

    @Data
    public static class DaySchedule {
        private String dayOfWeek; // e.g., "MONDAY", "TUESDAY", etc.
        private String startTime; // e.g., "09:00"
        private String endTime; // e.g., "17:00"
    }
}
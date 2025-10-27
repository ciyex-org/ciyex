package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentDTO {
    private Long id;
    private String visitType;
    private Long patientId;
    private Long providerId;

    // ✅ Use proper date/time with JSON formatting
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentEndDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime appointmentStartTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime appointmentEndTime;

    // ✅ Pre-formatted for frontend (optional convenience)
    private String formattedDate;   // e.g. "09/22/2025"
    private String formattedTime;   // e.g. "09:00 AM"

    private String priority;
    private Long locationId;   // Location reference
    private String status;     // PENDING / CONFIRMED / AVAILABLE
    private String reason;
    private Long orgId;

    // Telehealth
    // private String meetingUrl;

    // ✅ Track audit metadata
    private Audit audit = new Audit();

    @Data
    public static class Audit {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private String createdDate;       // ISO string or timestamp
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private String lastModifiedDate;  // ISO string or timestamp
    }
}

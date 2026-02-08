package org.ciyex.ehr.dto.portal;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    private String visitType;
    private Long patientId;
    private Long providerId;
    private LocalDate appointmentStartDate;
    private LocalDate appointmentEndDate;
    private LocalTime appointmentStartTime;
    private LocalTime appointmentEndTime;
    private String formattedDate;
    private String formattedTime;
    private String priority;
    private Long locationId;
    private String status;
    private String reason;
    private String providerName;
    private String locationName;
    private String patientName;
    private String meetingUrl;
}
package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class AppointmentDTO {
    private Long id;
    private String visitType;
    private Long patientId;
    private Long providerId;
    private String appointmentStartDate;
    private String appointmentEndDate;
    private String appointmentStartTime;
    private String appointmentEndTime;
    private String priority;
    private Long locationId;   // Changed from String location → Long locationId
    private String status;
    private String reason;
    private Long orgId;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

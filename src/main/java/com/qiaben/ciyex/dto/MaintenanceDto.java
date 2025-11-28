package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class MaintenanceDto {
    private Long id;

    private String equipment;
    private String category;   // Preventive, Corrective, Calibration, Cleaning
    private String location;   // Department / Room
    private String dueDate;
    private String lastServiceDate;

    private String assignee;
    private String vendor;
    private String priority;   // Critical, High, Medium, Low
    private String status;     // Open, In Progress, Done

    private String notes;

    private Audit audit;
    private String externalId;
    private String fhirId;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}

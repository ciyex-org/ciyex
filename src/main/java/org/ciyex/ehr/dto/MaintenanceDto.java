package org.ciyex.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenanceDto {
    private Long id;

    @NotBlank(message = "Equipment is required")
    private String equipment;
    
    @NotBlank(message = "Category is required")
    private String category;   // Preventive, Corrective, Calibration, Cleaning
    
    @NotBlank(message = "Location is required")
    private String location;   // Department / Room
    
    private String dueDate;
    private String lastServiceDate;

    private String assignee;
    private String vendor;
    
    @NotBlank(message = "Priority is required")
    private String priority;   // Critical, High, Medium, Low
    
    @NotBlank(message = "Status is required")
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

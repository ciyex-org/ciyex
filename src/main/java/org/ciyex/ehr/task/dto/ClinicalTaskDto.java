package org.ciyex.ehr.task.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClinicalTaskDto {
    private Long id;
    private String title;
    private String description;
    private String taskType;
    private String status;
    private String priority;
    private String dueDate;
    private String dueTime;
    private String assignedTo;
    private String assignedBy;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String referenceType;
    private Long referenceId;
    private String completedAt;
    private String completedBy;
    private String notes;
    private String createdAt;
    private String updatedAt;
}

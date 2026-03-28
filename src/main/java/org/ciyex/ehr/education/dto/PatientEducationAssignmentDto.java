package org.ciyex.ehr.education.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientEducationAssignmentDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long materialId;
    private String materialTitle;
    private String materialCategory;
    private String materialContentType;
    private String assignedBy;
    private String assignedDate;
    private String dueDate;
    private String status;
    private String viewedAt;
    private String completedAt;
    private Long encounterId;
    private String notes;
    private String patientFeedback;
    private String createdAt;
    private String updatedAt;
}

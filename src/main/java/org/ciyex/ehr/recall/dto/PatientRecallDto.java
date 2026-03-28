package org.ciyex.ehr.recall.dto;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientRecallDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private Long recallTypeId;
    private String recallTypeName;
    private String recallTypeCode;
    private String recallTypeCategory;
    private Long providerId;
    private String providerName;
    private Long locationId;
    private String status;
    private String dueDate;
    private String notificationDate;
    private String sourceEncounterId;
    private Long sourceAppointmentId;
    private Long linkedAppointmentId;
    private String completedEncounterId;
    private String completedDate;
    private Integer attemptCount;
    private String lastAttemptDate;
    private String lastAttemptMethod;
    private String lastAttemptOutcome;
    private String nextAttemptDate;
    private String preferredContact;
    private String priority;
    private String notes;
    private String cancelledReason;
    private Boolean autoCreated;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;

    // Nested outreach logs (included in detail view)
    private List<RecallOutreachLogDto> outreachLogs;
}

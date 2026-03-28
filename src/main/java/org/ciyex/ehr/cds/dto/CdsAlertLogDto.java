package org.ciyex.ehr.cds.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CdsAlertLogDto {
    private Long id;
    private Long ruleId;
    private String ruleName;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String alertType;
    private String severity;
    private String message;
    private String actionTaken;
    private String overrideReason;
    private String actedBy;
    private String actedAt;
    private String createdAt;
}

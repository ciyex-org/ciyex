package org.ciyex.ehr.audit.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String userId;
    private String userName;
    private String userRole;
    private String ipAddress;
    private String details;
    private Long patientId;
    private String patientName;
    private String createdAt;
}

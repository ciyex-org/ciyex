package org.ciyex.ehr.portal.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PortalAccessRequestDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private String status;
    private String approvedBy;
    private String approvedAt;
    private String deniedReason;
    private String createdAt;
    private String updatedAt;
}

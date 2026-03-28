package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPreferenceDto {
    private Long id;
    private String eventType;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private String timing;
    private Long templateId;
    private String createdAt;
    private String updatedAt;
}

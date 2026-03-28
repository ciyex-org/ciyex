package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationLogDto {
    private Long id;
    private String channelType;
    private String recipient;
    private String recipientName;
    private String templateKey;
    private String subject;
    private String body;
    private String status;
    private String errorMessage;
    private String externalId;
    private Long patientId;
    private String patientName;
    private String sentBy;
    private String triggerType;
    private String metadata;
    private String sentAt;
    private String deliveredAt;
    private String createdAt;
}

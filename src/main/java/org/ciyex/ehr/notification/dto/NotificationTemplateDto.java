package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationTemplateDto {
    private Long id;
    private String name;
    private String templateKey;
    private String channelType;
    private String subject;
    private String body;
    private String htmlBody;
    private Boolean isActive;
    private Boolean isDefault;
    private Object variables;   // accepts array or string from frontend
    private String createdAt;
    private String updatedAt;
}

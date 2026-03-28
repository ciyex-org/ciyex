package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationConfigDto {
    private Long id;
    private String channelType;
    private String provider;
    private Boolean enabled;
    private String config;
    private String senderName;
    private String senderAddress;
    private Integer dailyLimit;
    private Integer sentToday;
    private String lastResetDate;
    private String createdAt;
    private String updatedAt;
}

package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkCampaignDto {
    private Long id;
    private String name;
    private String channelType;
    private Long templateId;
    private String subject;
    private String body;
    private String targetCriteria;
    private Integer totalRecipients;
    private Integer sentCount;
    private Integer failedCount;
    private String status;
    private String scheduledAt;
    private String startedAt;
    private String completedAt;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}

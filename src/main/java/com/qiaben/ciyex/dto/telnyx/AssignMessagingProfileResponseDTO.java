package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignMessagingProfileResponseDTO {
    private String messagingProfileId;
    private String tcrCampaignId;
    private String campaignId;
    private String taskId; // returned by Telnyx (202 Accepted)
}
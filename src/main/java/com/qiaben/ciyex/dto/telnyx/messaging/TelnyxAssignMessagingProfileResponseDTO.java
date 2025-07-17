package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxAssignMessagingProfileResponseDTO {
    private String messagingProfileId;
    private String tcrCampaignId;
    private String campaignId;
    private String taskId; // returned by Telnyx (202 Accepted)
}
package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxAssignMessagingProfileRequestDTO {
    private String messagingProfileId; // required
    private String tcrCampaignId;      // mutually-exclusive with campaignId
    private String campaignId;         // mutually-exclusive with tcrCampaignId
}
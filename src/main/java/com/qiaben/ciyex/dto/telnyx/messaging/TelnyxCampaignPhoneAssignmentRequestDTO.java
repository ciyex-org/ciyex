package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TelnyxCampaignPhoneAssignmentRequestDTO {
    private String phoneNumber;
    private String campaignId;
}

package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignPhoneAssignmentRequestDTO {
    private String phoneNumber;
    private String campaignId;
}

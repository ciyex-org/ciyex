package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignPhoneAssignmentRecordDTO {
    private String phoneNumber;
    private String brandId;
    private String tcrBrandId;
    private String campaignId;
    private String tcrCampaignId;
    private String telnyxCampaignId;
    private String assignmentStatus;
    private String failureReasons;
    private String createdAt;
    private String updatedAt;
}

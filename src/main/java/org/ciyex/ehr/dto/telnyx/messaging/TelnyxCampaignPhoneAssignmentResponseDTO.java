package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TelnyxCampaignPhoneAssignmentResponseDTO {
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

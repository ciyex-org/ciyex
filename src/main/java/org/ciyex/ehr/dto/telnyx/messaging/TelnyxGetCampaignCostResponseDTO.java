package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxGetCampaignCostResponseDTO {
    private String campaignUsecase;  // Required
    private String monthlyCost;      // Required
    private String upFrontCost;      // Required
    private String description;      // Required
}

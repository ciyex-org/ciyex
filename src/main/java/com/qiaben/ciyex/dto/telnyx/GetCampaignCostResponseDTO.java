package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class GetCampaignCostResponseDTO {
    private String campaignUsecase;  // Required
    private String monthlyCost;      // Required
    private String upFrontCost;      // Required
    private String description;      // Required
}

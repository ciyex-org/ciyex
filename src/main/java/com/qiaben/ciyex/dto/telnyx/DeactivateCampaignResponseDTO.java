package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class DeactivateCampaignResponseDTO {
    private Long time;             // Epoch timestamp
    private String record_type;    // Should be "campaign"
    private String message;        // e.g. "Campaign deactivated successfully"
}

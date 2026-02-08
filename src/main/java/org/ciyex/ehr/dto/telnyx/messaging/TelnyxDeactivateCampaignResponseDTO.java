package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxDeactivateCampaignResponseDTO {
    private Long time;             // Epoch timestamp
    private String record_type;    // Should be "campaign"
    private String message;        // e.g. "Campaign deactivated successfully"
}

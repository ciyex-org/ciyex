package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxUpdateSharedCampaignRequestDTO {
    private String webhookURL;
    private String webhookFailoverURL;
}

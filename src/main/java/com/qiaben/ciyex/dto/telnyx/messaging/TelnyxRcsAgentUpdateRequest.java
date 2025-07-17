package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelnyxRcsAgentUpdateRequest {
    private String profileId;
    private String webhookUrl;
    private String webhookFailoverUrl;
}

package com.qiaben.ciyex.dto.telnyx;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RcsAgentUpdateRequest {
    private String profileId;
    private String webhookUrl;
    private String webhookFailoverUrl;
}

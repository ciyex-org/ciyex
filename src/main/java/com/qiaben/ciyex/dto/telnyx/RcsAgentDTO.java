package com.qiaben.ciyex.dto.telnyx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcsAgentDTO {
    private String agentId;
    private String userId;
    private String profileId;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private String agentName;
    private boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelnyxRcsAgentDTO {
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

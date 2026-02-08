package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

// DTO for PATCH body
@Data
public class TelnyxUpdateShortCodeRequest {
    private String messaging_profile_id;
}

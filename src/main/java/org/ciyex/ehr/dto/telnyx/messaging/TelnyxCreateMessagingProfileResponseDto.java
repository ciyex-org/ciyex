package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.Map;

@Data
public class TelnyxCreateMessagingProfileResponseDto {
    private Map<String, Object> data;
}

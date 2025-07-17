package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;
import java.util.Map;

@Data
public class TelnyxSubmitCampaignResponseDTO {
    private Map<String, Object> data; // for flexible response handling
}

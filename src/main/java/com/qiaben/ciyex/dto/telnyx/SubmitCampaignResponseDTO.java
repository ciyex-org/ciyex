package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class SubmitCampaignResponseDTO {
    private Map<String, Object> data; // for flexible response handling
}

package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.Map;

@Data
public class GetOsrCampaignAttributesResponseDTO {
    // Dynamic or unknown schema response
    private Map<String, Object> data;
}

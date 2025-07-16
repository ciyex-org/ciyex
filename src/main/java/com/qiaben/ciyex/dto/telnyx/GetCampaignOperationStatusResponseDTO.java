package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.Map;

@Data
public class GetCampaignOperationStatusResponseDTO {
    // You can expand this later as per real fields from Telnyx response
    private Map<String, Object> data; // catch-all if fields are dynamic
}

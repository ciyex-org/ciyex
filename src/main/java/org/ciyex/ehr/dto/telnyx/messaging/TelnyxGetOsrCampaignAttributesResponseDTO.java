package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

import java.util.Map;

@Data
public class TelnyxGetOsrCampaignAttributesResponseDTO {
    // Dynamic or unknown schema response
    private Map<String, Object> data;
}

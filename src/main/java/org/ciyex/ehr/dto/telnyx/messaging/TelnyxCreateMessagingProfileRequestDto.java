package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TelnyxCreateMessagingProfileRequestDto {
    private String name;
    private List<String> whitelisted_destinations;
    private Boolean enabled = true;
    private String webhook_url;
    private String webhook_failover_url;
    private String webhook_api_version = "2";
    private Map<String, Object> number_pool_settings;
    private Map<String, Object> url_shortener_settings;
    private String alpha_sender;
    private String daily_spend_limit;
    private Boolean daily_spend_limit_enabled;
    private Boolean mms_fall_back_to_sms;
    private Boolean mms_transcoding;
}

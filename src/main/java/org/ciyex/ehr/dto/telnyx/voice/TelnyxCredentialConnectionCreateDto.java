package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxCredentialConnectionCreateDto {
    private boolean active = true;
    private String user_name;
    private String password;
    private String connection_name;
    private String anchorsite_override = "Latency";
    private String sip_uri_calling_preference;
    private boolean default_on_hold_comfort_noise_enabled;
    private String dtmf_type;
    private boolean encode_contact_header_enabled;
    private String encrypted_media;
    private boolean onnet_t38_passthrough_enabled;
    private String ios_push_credential_id;
    private String android_push_credential_id;
    private String webhook_event_url;
    private String webhook_event_failover_url;
    private String webhook_api_version = "1";
    private Integer webhook_timeout_secs;
    private List<String> tags;
    private Object rtcp_settings;
    private Object inbound;
    private Object outbound;
}

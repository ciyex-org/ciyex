package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxExternalConnectionDTO {

    // Common request/update fields
    private Boolean active = true;
    private String external_sip_connection; // Required for Create
    private List<String> tags;
    private String webhook_event_url;
    private String webhook_event_failover_url;
    private Integer webhook_timeout_secs;

    private Inbound inbound;
    private Outbound outbound;

    // Response-only fields
    private Long id;
    private String record_type;
    private Boolean credential_active;
    private String webhook_api_version; // only present in response
    private String created_at;
    private String updated_at;

    @Data
    public static class Inbound {
        private Integer channel_limit;
    }

    @Data
    public static class Outbound {
        private Integer channel_limit;
        private Long outbound_voice_profile_id; // Required for create/update
    }
}

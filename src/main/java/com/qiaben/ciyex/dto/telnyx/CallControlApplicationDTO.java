package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

import java.util.List;

/**
 * Represents a Telnyx Call-Control Application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallControlApplicationDTO {

    /* ───────── top-level fields ───────── */
    private String  application_name;           // required for create/update
    private String  webhook_event_url;          // required for create/update
    private Boolean active;
    private String  anchorsite_override;        // "Latency", "Chicago, IL", …
    private String  dtmf_type;                  // "RFC 2833", "Inband", "SIP INFO"
    private Boolean first_command_timeout;
    private Integer first_command_timeout_secs;
    private Inbound inbound;                    // nested block
    private Outbound outbound;                  // nested block
    private List<String> tags;
    private Integer webhook_timeout_secs;
    private String  webhook_event_failover_url;
    private String  webhook_api_version;        // "1" or "2"
    private Boolean redact_dtmf_debug_logging;

    /* ───────── nested objects ───────── */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Inbound {
        private Integer channel_limit;
        private Boolean shaken_stir_enabled;
        private String  sip_subdomain;
        private String  sip_subdomain_receive_settings; // only_my_connections | from_anyone
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Outbound {
        private Integer channel_limit;
        private Long    outbound_voice_profile_id;
    }
}


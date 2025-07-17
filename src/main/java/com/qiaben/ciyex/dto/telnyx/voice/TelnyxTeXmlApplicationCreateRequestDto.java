package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxTeXmlApplicationCreateRequestDto {
    private String friendly_name;
    private Boolean active = true;
    private String anchorsite_override = "Latency";
    private String dtmf_type = "RFC 2833";
    private Boolean first_command_timeout;
    private Integer first_command_timeout_secs = 30;
    private List<String> tags;
    private String voice_url;
    private String voice_fallback_url;
    private String voice_method = "post";
    private String status_callback;
    private String status_callback_method = "post";
    private Inbound inbound;
    private Outbound outbound;

    @Data
    public static class Inbound {
        private Integer channel_limit;
        private Boolean shaken_stir_enabled;
        private String sip_subdomain;
        private String sip_subdomain_receive_settings;
    }

    @Data
    public static class Outbound {
        private Integer channel_limit;
        private Long outbound_voice_profile_id;
    }
}

package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxIpConnectionResponseDto {
    private List<IpConnectionData> data;
    private Meta meta;

    @Data
    public static class IpConnectionData {
        private Long id;
        private String record_type;
        private Boolean active;
        private String anchorsite_override;
        private String connection_name;
        private String transport_protocol;
        private Boolean default_on_hold_comfort_noise_enabled;
        private String dtmf_type;
        private Boolean encode_contact_header_enabled;
        private String encrypted_media;
        private Boolean onnet_t38_passthrough_enabled;
        private String webhook_event_url;
        private String webhook_event_failover_url;
        private String webhook_api_version;
        private Integer webhook_timeout_secs;
        private RtcpSettings rtcp_settings;
        private String created_at;
        private String updated_at;
        private List<String> tags;
        private InboundSettings inbound;
        private OutboundSettings outbound;

        @Data
        public static class RtcpSettings {
            private String port;
            private Boolean capture_enabled;
            private Integer report_frequency_secs;
        }

        @Data
        public static class InboundSettings {
            private String ani_number_format;
            private String dnis_number_format;
            private List<String> codecs;
            private String default_primary_ip_id;
            private String default_secondary_ip_id;
            private String default_tertiary_ip_id;
            private String default_routing_method;
            private Integer channel_limit;
            private Boolean generate_ringback_tone;
            private Boolean isup_headers_enabled;
            private Boolean prack_enabled;
            private Boolean sip_compact_headers_enabled;
            private String sip_region;
            private String sip_subdomain;
            private String sip_subdomain_receive_settings;
            private Integer timeout_1xx_secs;
            private Integer timeout_2xx_secs;
            private Boolean shaken_stir_enabled;
        }

        @Data
        public static class OutboundSettings {
            private Boolean call_parking_enabled;
            private String ani_override;
            private String ani_override_type;
            private Integer channel_limit;
            private Boolean instant_ringback_enabled;
            private Boolean generate_ringback_tone;
            private String localization;
            private String t38_reinvite_source;
            private String tech_prefix;
            private String ip_authentication_method;
            private String ip_authentication_token;
            private Long outbound_voice_profile_id;
        }
    }

    @Data
    public static class Meta {
        private int total_pages;
        private int total_results;
        private int page_number;
        private int page_size;
    }
}

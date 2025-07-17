package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxTeXmlApplicationListResponseDto {
    private List<TeXmlApplicationDto> data;
    private MetaDto meta;

    @Data
    public static class TeXmlApplicationDto {
        private Long id;
        private String record_type;
        private String friendly_name;
        private Boolean active;
        private String anchorsite_override;
        private String dtmf_type;
        private Boolean first_command_timeout;
        private Integer first_command_timeout_secs;
        private String voice_url;
        private String voice_fallback_url;
        private String voice_method;
        private String status_callback;
        private String status_callback_method;
        private List<String> tags;
        private Inbound inbound;
        private Outbound outbound;
        private String created_at;
        private String updated_at;

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

    @Data
    public static class MetaDto {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }
}

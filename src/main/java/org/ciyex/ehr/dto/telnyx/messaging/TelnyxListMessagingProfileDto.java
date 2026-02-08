package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TelnyxListMessagingProfileDto {
    private List<MessagingProfileData> data;
    private Meta meta;
    private List<Error> errors;

    @Data
    public static class MessagingProfileData {
        private String record_type;
        private String id;
        private Boolean mms_fall_back_to_sms;
        private Boolean mms_transcoding;
        private String name;
        private Boolean enabled;
        private String webhook_url;
        private String webhook_failover_url;
        private String webhook_api_version;
        private List<String> whitelisted_destinations;
        private String created_at;
        private String updated_at;
        private String v1_secret;
        private Map<String, Object> number_pool_settings;
        private Map<String, Object> url_shortener_settings;
        private String alpha_sender;
        private String daily_spend_limit;
        private Boolean daily_spend_limit_enabled;
    }

    @Data
    public static class Meta {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }

    @Data
    public static class Error {
        private Integer code;
        private String title;
        private String detail;
        private Source source;
        private Map<String, Object> meta;

        @Data
        public static class Source {
            private String pointer;
            private String parameter;
        }
    }
}

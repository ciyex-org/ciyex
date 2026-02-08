package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxConnectionResponseDto {
    private List<ConnectionData> data;
    private Meta meta;

    @Data
    public static class ConnectionData {
        private Long id;
        private String record_type;
        private Boolean active;
        private String anchorsite_override;
        private String connection_name;
        private String created_at;
        private String updated_at;
        private String webhook_event_url;
        private String webhook_event_failover_url;
        private String webhook_api_version;
        private Long outbound_voice_profile_id;
        private List<String> tags;
    }

    @Data
    public static class Meta {
        private int total_pages;
        private int total_results;
        private int page_number;
        private int page_size;
    }
}

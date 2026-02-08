package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class TelnyxCreateRoomCompositionResponseDto {
    private RoomCompositionData data;

    @Data
    public static class RoomCompositionData {
        private String id;
        private String room_id;
        private String session_id;
        private String user_id;
        private String status;
        private Float size_mb;
        private String download_url;
        private Integer duration_secs;
        private String format;
        private OffsetDateTime created_at;
        private OffsetDateTime updated_at;
        private OffsetDateTime ended_at;
        private OffsetDateTime started_at;
        private OffsetDateTime completed_at;
        private Map<String, TelnyxCreateRoomCompositionRequestDto.VideoRegion> video_layout;
        private String webhook_event_url;
        private String webhook_event_failover_url;
        private Integer webhook_timeout_secs;
        private String record_type;
    }
}

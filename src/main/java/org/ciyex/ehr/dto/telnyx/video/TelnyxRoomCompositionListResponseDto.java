package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TelnyxRoomCompositionListResponseDto {
    private List<RoomCompositionData> data;
    private Meta meta;

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
        private Map<String, VideoRegion> video_layout;
        private String webhook_event_url;
        private String webhook_event_failover_url;
        private Integer webhook_timeout_secs;
        private String record_type;
    }

    @Data
    public static class VideoRegion {
        private Integer x_pos;
        private Integer y_pos;
        private Integer z_pos;
        private Integer height;
        private Integer width;
        private Integer max_columns;
        private Integer max_rows;
        private List<String> video_sources;
    }

    @Data
    public static class Meta {
        private Integer page_number;
        private Integer page_size;
        private Integer total_pages;
        private Integer total_results;
    }
}

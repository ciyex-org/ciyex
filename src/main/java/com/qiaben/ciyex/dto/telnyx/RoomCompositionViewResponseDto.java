package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class RoomCompositionViewResponseDto {
    private UUID id;
    private UUID room_id;
    private UUID session_id;
    private UUID user_id;
    private String status;
    private Float size_mb;
    private String download_url;
    private Integer duration_secs;
    private String format;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private LocalDateTime ended_at;
    private LocalDateTime started_at;
    private LocalDateTime completed_at;
    private Map<String, VideoRegion> video_layout;
    private String webhook_event_url;
    private String webhook_event_failover_url;
    private Integer webhook_timeout_secs;
    private String record_type;

    @Data
    public static class VideoRegion {
        private Integer x_pos;
        private Integer y_pos;
        private Integer z_pos;
        private Integer height;
        private Integer width;
        private Integer max_columns;
        private Integer max_rows;
        private UUID[] video_sources;
    }
}

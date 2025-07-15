package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateRoomCompositionRequestDto {
    private String format = "mp4";
    private String resolution = "1280x720";
    private String session_id;
    private Map<String, VideoRegion> video_layout;
    private String webhook_event_url;
    private String webhook_event_failover_url;
    private Integer webhook_timeout_secs;

    @Data
    public static class VideoRegion {
        private Integer x_pos = 0;
        private Integer y_pos = 0;
        private Integer z_pos = 0;
        private Integer height;
        private Integer width;
        private Integer max_columns;
        private Integer max_rows;
        private List<String> video_sources;
    }
}

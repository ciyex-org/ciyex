package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomRecordingListResponseDto {
    private List<RoomRecordingDto> data;
    private Meta meta;

    @Data
    public static class RoomRecordingDto {
        private String id;
        private String room_id;
        private String session_id;
        private String participant_id;
        private String status;
        private String type;
        private Float size_mb;
        private String download_url;
        private String codec;
        private Integer duration_secs;
        private LocalDateTime created_at;
        private LocalDateTime updated_at;
        private LocalDateTime ended_at;
        private LocalDateTime started_at;
        private LocalDateTime completed_at;
        private String record_type;
    }

    @Data
    public static class Meta {
        private Integer page_number;
        private Integer page_size;
        private Integer total_pages;
        private Integer total_results;
    }
}

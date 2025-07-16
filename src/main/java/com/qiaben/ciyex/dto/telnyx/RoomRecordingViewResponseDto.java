package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RoomRecordingViewResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
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
        private String created_at;
        private String updated_at;
        private String ended_at;
        private String started_at;
        private String completed_at;
        private String record_type;
    }
}

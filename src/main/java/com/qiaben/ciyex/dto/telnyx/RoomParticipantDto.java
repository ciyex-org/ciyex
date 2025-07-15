package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomParticipantDto {
    private String id;
    private String session_id;
    private String context;
    private LocalDateTime joined_at;
    private LocalDateTime updated_at;
    private LocalDateTime left_at;
    private String record_type;

    @Data
    public static class RoomParticipantListResponse {
        private List<RoomParticipantDto> data;
        private Meta meta;

        @Data
        public static class Meta {
            private int page_number;
            private int page_size;
            private int total_pages;
            private int total_results;
        }
    }

    @Data
    public static class RoomParticipantSingleResponse {
        private RoomParticipantDto data;
    }
}

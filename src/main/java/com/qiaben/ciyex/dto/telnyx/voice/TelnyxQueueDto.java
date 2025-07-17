package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelnyxQueueDto {
    private String record_type;
    private String id;
    private String name;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private Integer current_size;
    private Integer max_size;
    private Integer average_wait_time_secs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueWrapper {
        private TelnyxQueueDto data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueCallDto {
        private String record_type;
        private String call_session_id;
        private String call_leg_id;
        private String call_control_id;
        private String connection_id;
        private String from;
        private String to;
        private OffsetDateTime enqueued_at;
        private Integer wait_time_secs;
        private Integer queue_position;
        private String queue_id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueCallWrapper {
        private QueueCallDto data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueCallListWrapper {
        private List<QueueCallDto> data;
    }
}

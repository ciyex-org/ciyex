package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxRoomSessionResponseDto {
    private SessionDto data;

    @Data
    public static class SessionDto {
        private String id;
        private String roomId;
        private Boolean active;
        private String createdAt;
        private String updatedAt;
        private String endedAt;
        private List<ParticipantDto> participants;
        private String recordType;
    }

    @Data
    public static class ParticipantDto {
        private String id;
        private String sessionId;
        private String context;
        private String joinedAt;
        private String updatedAt;
        private String leftAt;
        private String recordType;
    }
}

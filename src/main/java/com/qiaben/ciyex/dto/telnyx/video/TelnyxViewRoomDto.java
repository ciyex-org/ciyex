package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxViewRoomDto {
    private String id;
    private Integer maxParticipants;
    private String uniqueName;
    private String createdAt;
    private String updatedAt;
    private String activeSessionId;
    private List<SessionDto> sessions;
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;

    @Data
    public static class SessionDto {
        private String id;
        private String roomId;
        private Boolean active;
        private String createdAt;
        private String updatedAt;
        private String endedAt;
        private List<ParticipantDto> participants;
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

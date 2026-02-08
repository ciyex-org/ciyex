package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRoomResponseDto {
    private String id;
    private Integer maxParticipants;
    private String uniqueName;
    private String createdAt;
    private String updatedAt;
    private String activeSessionId;
    private SessionDto[] sessions;
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;

    // Nested Session DTO
    @Data
    public static class SessionDto {
        private String id;
        private String roomId;
        private Boolean active;
        private String createdAt;
        private String updatedAt;
        private String endedAt;
        private ParticipantDto[] participants;
    }

    // Nested Participant DTO
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

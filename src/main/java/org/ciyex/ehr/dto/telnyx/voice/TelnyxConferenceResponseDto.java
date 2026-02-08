package org.ciyex.ehr.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Wrapper for Telnyx conference response (200 OK).
 */
@Data
public class TelnyxConferenceResponseDto {

    private DataDto data;

    @Data
    public static class DataDto {
        @JsonProperty("record_type")
        private String recordType;  // "conference"

        private String id;
        private String name;

        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("expires_at")
        private String expiresAt;
        @JsonProperty("updated_at")
        private String updatedAt;

        private String region;
        private String status;

        @JsonProperty("end_reason")
        private String endReason;

        @JsonProperty("ended_by")
        private EndedByDto endedBy;

        @JsonProperty("connection_id")
        private String connectionId;
    }

    @Data
    public static class EndedByDto {
        @JsonProperty("connection_id")
        private String connectionId;
        @JsonProperty("call_control_id")
        private String callControlId;
        @JsonProperty("participant_id")
        private String participantId;
    }
}


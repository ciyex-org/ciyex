package org.ciyex.ehr.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Shrunk response model – keep only what you actually use.
 * You can extend later (Jackson will happily ignore unknown fields).
 */
@Data
public class TelnyxDialResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        @JsonProperty("record_type")
        private String recordType;

        @JsonProperty("call_session_id")
        private String callSessionId;

        @JsonProperty("call_leg_id")
        private String callLegId;

        @JsonProperty("call_control_id")
        private String callControlId;

        @JsonProperty("is_alive")
        private Boolean alive;

        @JsonProperty("recording_id")
        private String recordingId;
    }
}

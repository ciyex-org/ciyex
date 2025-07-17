package com.qiaben.ciyex.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelnyxAnswerResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;

        @JsonProperty("recording_id")
        private String recordingId;
    }
}

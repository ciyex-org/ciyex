package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxPlaybackStartResponseDTO {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private String result;
    }
}

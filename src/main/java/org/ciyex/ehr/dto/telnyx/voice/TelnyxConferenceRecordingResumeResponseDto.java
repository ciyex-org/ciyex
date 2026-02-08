package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxConferenceRecordingResumeResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private String result;
    }
}

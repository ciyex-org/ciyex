package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUpdateConferenceParticipantResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private String result;
    }
}

package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxUnmuteParticipantsResponseDto {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private String result;
    }
}

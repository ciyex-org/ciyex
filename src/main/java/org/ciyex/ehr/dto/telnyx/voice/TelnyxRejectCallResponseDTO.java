package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxRejectCallResponseDTO {
    private DataPayload data;

    @Data
    public static class DataPayload {
        private String result;
    }
}

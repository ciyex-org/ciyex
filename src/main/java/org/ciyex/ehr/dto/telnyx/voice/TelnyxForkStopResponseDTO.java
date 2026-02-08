package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxForkStopResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;
    }
}

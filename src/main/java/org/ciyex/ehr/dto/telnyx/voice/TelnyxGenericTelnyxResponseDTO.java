package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxGenericTelnyxResponseDTO {
    private DataNode data;

    @Data
    public static class DataNode {
        private String result;
    }
}


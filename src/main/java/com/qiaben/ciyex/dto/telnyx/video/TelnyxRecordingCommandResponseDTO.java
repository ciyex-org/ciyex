package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRecordingCommandResponseDTO {
    private DataNode data;

    @Data
    public static class DataNode {
        private String result;
    }
}


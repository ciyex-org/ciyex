package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RecordingCommandResponseDTO {
    private DataNode data;

    @Data
    public static class DataNode {
        private String result;
    }
}


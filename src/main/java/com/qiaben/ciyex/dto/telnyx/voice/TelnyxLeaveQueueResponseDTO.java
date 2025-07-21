package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxLeaveQueueResponseDTO {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private String result;
    }
}

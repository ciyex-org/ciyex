package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxRejectCallResponseDTO {
    private DataPayload data;

    @Data
    public static class DataPayload {
        private String result;
    }
}

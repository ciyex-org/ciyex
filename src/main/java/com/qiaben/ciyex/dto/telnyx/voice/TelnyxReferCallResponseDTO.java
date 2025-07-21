package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxReferCallResponseDTO {
    private DataResult data;

    @Data
    public static class DataResult {
        private String result;
    }
}

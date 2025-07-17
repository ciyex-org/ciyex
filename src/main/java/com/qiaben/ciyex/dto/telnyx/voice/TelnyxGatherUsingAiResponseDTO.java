package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

/**
 * Minimal response wrapper – extend as needed.
 */
@Data
public class TelnyxGatherUsingAiResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;
    }
}

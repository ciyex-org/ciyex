package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxAiAssistantResponseDTO {
    private DataDTO data;

    @Data
    public static class DataDTO {
        private String result;
    }
}

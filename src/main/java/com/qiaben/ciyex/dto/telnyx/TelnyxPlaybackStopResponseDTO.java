package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxPlaybackStopResponseDTO {
    private DataDTO data;

    @Data
    public static class DataDTO {
        private String result;
    }
}

package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxGatherAudioResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;
    }
}

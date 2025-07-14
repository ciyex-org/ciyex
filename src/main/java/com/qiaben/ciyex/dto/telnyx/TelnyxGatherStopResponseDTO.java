package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxGatherStopResponseDTO {
    private Data data;

    @lombok.Data
    public static class Data {
        private String result;
    }
}

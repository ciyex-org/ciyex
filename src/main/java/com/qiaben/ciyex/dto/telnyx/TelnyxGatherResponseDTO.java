package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxGatherResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;
    }
}

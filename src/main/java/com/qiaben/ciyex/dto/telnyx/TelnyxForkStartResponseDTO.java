package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxForkStartResponseDTO {

    private DataBlock data;

    @Data
    public static class DataBlock {
        private String result;
    }
}

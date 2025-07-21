package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxActionResultDto {
    private DataDto data;

    @Data
    public static class DataDto {
        private String result;
    }
}
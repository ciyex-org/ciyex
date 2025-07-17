package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxEndRoomSessionResponseDto {
    private DataDto data;

    @Data
    public static class DataDto {
        private String result;
    }
}

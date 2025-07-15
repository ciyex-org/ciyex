package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class EndRoomSessionResponseDto {
    private DataDto data;

    @Data
    public static class DataDto {
        private String result;
    }
}

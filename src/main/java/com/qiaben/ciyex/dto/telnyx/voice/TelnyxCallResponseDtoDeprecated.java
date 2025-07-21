package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxCallResponseDtoDeprecated {
    private DataDto data;

    @Data
    public static class DataDto {
        private String sid;
        private String from;
        private String to;
        private String status;
    }
}


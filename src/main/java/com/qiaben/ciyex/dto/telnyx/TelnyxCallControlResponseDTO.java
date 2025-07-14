package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxCallControlResponseDTO {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private String result;
    }
}

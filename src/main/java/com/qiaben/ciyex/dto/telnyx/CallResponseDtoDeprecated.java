package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class CallResponseDtoDeprecated {
    private DataDto data;

    @Data
    public static class DataDto {
        private String sid;
        private String from;
        private String to;
        private String status;
    }
}


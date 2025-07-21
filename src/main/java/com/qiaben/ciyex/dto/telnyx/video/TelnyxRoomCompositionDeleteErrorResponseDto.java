package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxRoomCompositionDeleteErrorResponseDto {
    private List<ErrorItem> errors;

    @Data
    public static class ErrorItem {
        private int code;
        private String title;
        private String detail;
        private Source source;
        private Object meta;

        @Data
        public static class Source {
            private String pointer;
            private String parameter;
        }
    }
}

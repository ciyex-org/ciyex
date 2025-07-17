package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxActiveCallListResponseDto {
    private List<ActiveCall> data;
    private Meta meta;

    @Data
    public static class ActiveCall {
        private String recordType;
        private String callSessionId;
        private String callLegId;
        private String callControlId;
        private String clientState;
        private Integer callDuration;
    }

    @Data
    public static class Meta {
        private Cursors cursors;
        private Integer totalItems;
        private String next;
        private String previous;

        @Data
        public static class Cursors {
            private String after;
            private String before;
        }
    }
}


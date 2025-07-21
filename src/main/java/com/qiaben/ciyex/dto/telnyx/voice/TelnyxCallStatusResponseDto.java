package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxCallStatusResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private String recordType;
        private String callSessionId;
        private String callLegId;
        private String callControlId;
        private Boolean isAlive;
        private String clientState;
        private Integer callDuration;
        private String startTime;
        private String endTime;
    }
}

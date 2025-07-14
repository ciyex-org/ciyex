package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class StartSiprecSessionRequestDto {
    private String connectorName;
    private String name;
    private String track;
    private Boolean includeMetadataCustomHeaders;
    private Boolean secure;
    private Integer sessionTimeoutSecs;
    private String sipTransport;
    private String statusCallback;
    private String statusCallbackMethod;
}


package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxStartSiprecSessionRequestDto {
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


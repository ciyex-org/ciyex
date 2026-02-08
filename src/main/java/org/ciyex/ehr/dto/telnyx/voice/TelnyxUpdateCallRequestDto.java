package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUpdateCallRequestDto {
    private String status;
    private String url;
    private String method;
    private String fallbackUrl;
    private String fallbackMethod;
    private String statusCallback;
    private String statusCallbackMethod;
    private String texml;
}

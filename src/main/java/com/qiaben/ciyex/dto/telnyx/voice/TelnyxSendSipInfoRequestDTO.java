package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxSendSipInfoRequestDTO {
    private String contentType;     // required
    private String body;            // required
    private String clientState;     // optional
    private String commandId;       // optional
}


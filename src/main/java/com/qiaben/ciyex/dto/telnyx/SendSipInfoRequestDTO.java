package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class SendSipInfoRequestDTO {
    private String contentType;     // required
    private String body;            // required
    private String clientState;     // optional
    private String commandId;       // optional
}


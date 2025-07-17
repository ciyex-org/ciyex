package com.qiaben.ciyex.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelnyxStreamResponseDto {
    @JsonProperty("account_sid")
    private String accountSid;

    @JsonProperty("call_sid")
    private String callSid;

    private String sid;
    private String name;
    private String status;

    @JsonProperty("date_updated")
    private String dateUpdated;

    private String uri;
}


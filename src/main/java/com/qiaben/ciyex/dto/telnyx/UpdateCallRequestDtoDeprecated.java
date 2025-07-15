package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UpdateCallRequestDtoDeprecated {
    private String Status;
    private String Url;
    private String Method;
    private String FallbackUrl;
    private String FallbackMethod;
    private String StatusCallback;
    private String StatusCallbackMethod;
    private String Texml;
}

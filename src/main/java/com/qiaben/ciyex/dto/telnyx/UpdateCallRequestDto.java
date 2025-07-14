package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UpdateCallRequestDto {
    private String status;
    private String url;
    private String method;
    private String fallbackUrl;
    private String fallbackMethod;
    private String statusCallback;
    private String statusCallbackMethod;
    private String texml;
}

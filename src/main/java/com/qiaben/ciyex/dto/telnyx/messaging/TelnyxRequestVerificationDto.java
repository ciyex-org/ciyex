package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxRequestVerificationDto {
    private String phone_number;
    private String verification_method; // "sms" or "call"
}

package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RequestVerificationDto {
    private String phone_number;
    private String verification_method; // "sms" or "call"
}

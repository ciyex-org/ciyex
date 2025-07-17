package com.qiaben.ciyex.service.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxResendBrand2FAResponseDto {
    private String brandId;
    private String message;
    private String status;
}

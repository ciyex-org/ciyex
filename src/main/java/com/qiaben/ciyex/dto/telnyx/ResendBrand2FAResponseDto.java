package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ResendBrand2FAResponseDto {
    private String brandId;
    private String message;
    private String status;
}

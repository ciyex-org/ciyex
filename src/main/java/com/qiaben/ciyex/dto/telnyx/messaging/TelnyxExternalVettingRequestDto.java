package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxExternalVettingRequestDto {
    private String evpId;
    private String vettingId;
    private String vettingToken;
}

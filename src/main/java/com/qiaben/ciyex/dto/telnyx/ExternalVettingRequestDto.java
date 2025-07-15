package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ExternalVettingRequestDto {
    private String evpId;
    private String vettingId;
    private String vettingToken;
}

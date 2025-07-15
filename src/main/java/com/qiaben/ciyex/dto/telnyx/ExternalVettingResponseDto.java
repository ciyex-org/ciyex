package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ExternalVettingResponseDto {
    private String evpId;
    private String vettingId;
    private String vettingToken;
    private Integer vettingScore;
    private String vettingClass;
    private String vettedDate;
    private String createDate;
}

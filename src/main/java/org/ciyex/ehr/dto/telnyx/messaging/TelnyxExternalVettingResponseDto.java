package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxExternalVettingResponseDto {
    private String evpId;
    private String vettingId;
    private String vettingToken;
    private Integer vettingScore;
    private String vettingClass;
    private String vettedDate;
    private String createDate;
}

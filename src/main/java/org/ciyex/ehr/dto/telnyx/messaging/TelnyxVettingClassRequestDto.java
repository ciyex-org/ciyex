package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxVettingClassRequestDto {
    private String evpId;
    private String vettingClass;
}

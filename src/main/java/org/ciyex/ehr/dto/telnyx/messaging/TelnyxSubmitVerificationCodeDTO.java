package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxSubmitVerificationCodeDTO {
    private String verification_code;
}

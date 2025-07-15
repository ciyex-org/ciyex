
// src/main/java/com/qiaben/ciyex/dto/telnyx/HostedNumberEligibilityResponseDto.java

package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class HostedNumberEligibilityResponseDto {

    private List<PhoneNumberStatus> phone_numbers;    // list element described below

    @Data
    public static class PhoneNumberStatus {
        private String phone_number;                  // +E.164
        private boolean eligible;                     // true | false
        private String eligible_status;               // ELIGIBLE | NUMBER_IS_NOT_IN_E164_FORMAT | …
        private String detail;                        // verbose explanation
    }
}


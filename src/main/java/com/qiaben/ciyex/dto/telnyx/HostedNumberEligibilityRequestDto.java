
// src/main/java/com/qiaben/ciyex/dto/telnyx/HostedNumberEligibilityRequestDto.java

package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

/** Payload →  POST /v2/messaging_hosted_numbers/eligibility */
@Data
public class HostedNumberEligibilityRequestDto {

    private List<String> phone_numbers;               // e.g. ["+12223334444","+12225556666"]
}


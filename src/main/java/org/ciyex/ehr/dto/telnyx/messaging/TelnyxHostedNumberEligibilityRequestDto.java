
// src/main/java/com/ciyex/ciyex/dto/telnyx/HostedNumberEligibilityRequestDto.java

package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

/** Payload →  POST /v2/messaging_hosted_numbers/eligibility */
@Data
public class TelnyxHostedNumberEligibilityRequestDto {

    private List<String> phone_numbers;               // e.g. ["+12223334444","+12225556666"]
}


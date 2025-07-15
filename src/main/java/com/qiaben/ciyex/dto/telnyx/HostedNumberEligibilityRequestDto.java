package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

/** Payload →  POST /v2/messaging_hosted_numbers/eligibility */
@Data
public class HostedNumberEligibilityRequestDto {
    private List<String> phone_numbers;
}


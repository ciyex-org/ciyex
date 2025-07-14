package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class NoiseSuppressionStartRequestDTO {
    /**
     * Base-64 string carried back in every webhook.
     */
    private String client_state;

    /**
     * Idempotency key – Telnyx will ignore duplicates for same call_control_id.
     */
    private String command_id;

    /**
     * Direction of audio to suppress: inbound | outbound | both (default inbound).
     */
    private String direction = "inbound";
}


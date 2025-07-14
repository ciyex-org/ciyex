package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxAiAssistantRequestDTO {
    private String client_state;
    private String command_id;
}

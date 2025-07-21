package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxRejectCallRequestDTO {
    private String client_state;
    private String command_id;
    private String cause; // Valid values: CALL_REJECTED, USER_BUSY
}

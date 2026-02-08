package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxHangupCallRequestDTO {
    private String client_state;
    private String command_id;
}

package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTranscriptionStopDTO {
    private String client_state;
    private String command_id;
}


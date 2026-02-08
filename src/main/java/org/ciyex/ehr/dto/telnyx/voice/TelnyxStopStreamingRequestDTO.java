package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxStopStreamingRequestDTO {
    private String client_state;
    private String command_id;
    private String stream_id;
}


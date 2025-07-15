package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TelnyxPlaybackStopRequestDTO {
    private Boolean overlay;
    private String stop = "all"; // Options: current, all
    private String client_state;
    private String command_id;
}

package com.qiaben.ciyex.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxForkStopRequestDTO {

    private String client_state;
    private String command_id;
    private String stream_type; // raw or decrypted (default raw)
}

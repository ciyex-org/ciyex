package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxForkStartRequestDTO {

    private String rx;             // UDP target for inbound media (e.g., udp:1.2.3.4:1234)
    private String tx;             // UDP target for outbound media (e.g., udp:1.2.3.4:5678)
    private String stream_type;    // decrypted
    private String client_state;
    private String command_id;
}

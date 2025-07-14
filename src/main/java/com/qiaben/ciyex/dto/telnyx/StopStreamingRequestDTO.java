package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class StopStreamingRequestDTO {
    private String client_state;
    private String command_id;
    private String stream_id;
}


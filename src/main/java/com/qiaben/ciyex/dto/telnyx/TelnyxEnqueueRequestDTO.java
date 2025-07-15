package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxEnqueueRequestDTO {
    private String queue_name;
    private String client_state;
    private String command_id;
    private Integer max_wait_time_secs;
    private Integer max_size;
}

package org.ciyex.ehr.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxGatherRequestDTO {

    private Integer minimum_digits;
    private Integer maximum_digits;
    private Integer timeout_millis;
    private Integer inter_digit_timeout_millis;
    private Integer initial_timeout_millis;
    private String terminating_digit;
    private String valid_digits;
    private String gather_id;
    private String client_state;
    private String command_id;
}

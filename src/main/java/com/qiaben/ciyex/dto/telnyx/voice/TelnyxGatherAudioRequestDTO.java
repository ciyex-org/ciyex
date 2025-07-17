package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxGatherAudioRequestDTO {
    private String client_state;
    private String command_id;
    private Integer maximum_digits;
    private Integer minimum_digits;
    private String terminator;
    private Integer timeout_millis;
    private String prompt_url;
    private List<String> valid_digits;
    private Boolean play_beep;
    private Boolean privacy_mode;
    private String language;
    private Boolean interruptible;
}

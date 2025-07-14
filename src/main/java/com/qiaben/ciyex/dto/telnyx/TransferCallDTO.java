package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TransferCallDTO {
    private String to; // Required
    private String from;
    private String from_display_name;
    private String audio_url;
    private Boolean early_media = true;
    private String media_name;
    private Integer timeout_secs = 30;
    private Integer time_limit_secs = 14400;
    private String client_state;
    private String target_leg_client_state;
    private String command_id;
}

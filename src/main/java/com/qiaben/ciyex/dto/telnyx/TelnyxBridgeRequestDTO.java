package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxBridgeRequestDTO {

    private String call_control_id;
    private String client_state;
    private String command_id;
    private String queue;
    private String video_room_id;
    private String video_room_context;
    private String park_after_unbridge;
    private Boolean play_ringtone;
    private String ringtone;

    private String record;
    private String record_channels;
    private String record_format;
    private Integer record_max_length;
    private Integer record_timeout_secs;
    private String record_track;
    private String record_trim;
    private String record_custom_file_name;

    private String mute_dtmf;
}

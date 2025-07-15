package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class StartStreamingRequestDTO {
    private String stream_url;
    private String stream_track = "inbound_track";
    private String stream_codec = "default";
    private String stream_bidirectional_mode = "mp3";
    private String stream_bidirectional_codec = "PCMU";
    private String stream_bidirectional_target_legs = "opposite";
    private Boolean enable_dialogflow = false;

    private DialogflowConfig dialogflow_config;
    private String client_state;
    private String command_id;

    @Data
    public static class DialogflowConfig {
        private Boolean analyze_sentiment;
        private Boolean partial_automated_agent_reply;
    }
}


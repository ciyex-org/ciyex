package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxPlaybackStartRequestDTO {
    private String audio_url;
    private String media_name;
    private Object loop; // Integer (1–100) or String "infinity"
    private Boolean overlay;
    private String stop;
    private String target_legs;
    private Boolean cache_audio = true;
    private String audio_type = "mp3"; // or "wav"
    private String playback_content;
    private String client_state;
    private String command_id;

    @Data
    public static class CustomHeader {
        private String name;
        private String value;
    }

    private List<CustomHeader> custom_headers;
}

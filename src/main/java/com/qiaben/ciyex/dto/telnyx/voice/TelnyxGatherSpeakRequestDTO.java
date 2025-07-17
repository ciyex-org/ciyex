package com.qiaben.ciyex.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxGatherSpeakRequestDTO {

    private String payload;
    private String invalid_payload;
    private String payload_type;
    private String service_level;
    private String voice;
    private VoiceSettings voice_settings;

    private Integer minimum_digits;
    private Integer maximum_digits;
    private Integer maximum_tries;
    private Integer timeout_millis;
    private String terminating_digit;
    private String valid_digits;
    private Integer inter_digit_timeout_millis;

    private String client_state;
    private String command_id;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VoiceSettings {
        private String api_key_ref;   // For ElevenLabs
        private String language;      // Language for voice synthesis
    }
}

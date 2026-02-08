package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TelnyxCallControlRequestDTO {

    // Common fields for most Telnyx call control commands
    private String client_state;
    private String command_id;

    // ========== GATHER USING AI specific ==========
    private Map<String, Object> parameters;
    private AssistantConfig assistant;
    private TranscriptionConfig transcription;
    private String greeting;
    private Boolean send_partial_results;
    private Boolean send_message_history_updates;
    private List<Message> message_history;
    private InterruptionSettings interruption_settings;
    private Integer user_response_timeout_ms;

    // ========== GATHER DTMF specific ==========
    private Integer minimum_digits;
    private Integer maximum_digits;
    private Integer timeout_millis;
    private Integer inter_digit_timeout_millis;
    private Integer initial_timeout_millis;
    private String terminating_digit;
    private String valid_digits;
    private String gather_id;

    // ========== FORK START specific ==========
    private String stream_url;
    private String stream_type;
    private Boolean audio_channels;

    // ========== Nested Config DTOs ==========
    @Data
    public static class AssistantConfig {
        private String model;
        private String instructions;
        private Map<String, Object> tools;
    }

    @Data
    public static class TranscriptionConfig {
        private String language;
        private String voice;
        private VoiceSettings voice_settings;
    }

    @Data
    public static class VoiceSettings {
        private String api_key_ref;
    }

    @Data
    public static class Message {
        private String content;
        private String role; // assistant | user
    }

    @Data
    public static class InterruptionSettings {
        private Boolean allow_interruptions;
    }
}

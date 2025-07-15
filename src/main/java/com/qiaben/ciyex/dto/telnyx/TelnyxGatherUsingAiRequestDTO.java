package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /v2/calls/{call_control_id}/actions/gather_using_ai
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxGatherUsingAiRequestDTO {

    /** JSON-Schema object that describes the fields to gather */
    private Map<String, Object> parameters;

    /** AI assistant configuration (LLM, instructions, tools) */
    private Assistant assistant;

    /** Transcription options */
    private Transcription transcription;

    /** Voice to use (e.g. "Telnyx.KokoroTTS.af" or "AWS.Polly.Joanna") */
    private String voice;

    /** Settings associated with the voice (e.g. ElevenLabs API key) */
    private VoiceSettings voice_settings;

    /** Greeting played at start (plain text or SSML) */
    private String greeting;

    private Boolean send_partial_results;
    private Boolean send_message_history_updates;

    /** Prior conversation context */
    private List<MessageHistory> message_history;

    private String client_state;
    private String command_id;

    /** User-interruption handling */
    private InterruptionSettings interruption_settings;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Assistant {
        private String model;               // e.g. "gpt-4o"
        private String instructions;        // system prompt / extra guidance
        private List<String> tools;         // list of enabled tools
    }

    @Data
    public static class Transcription {
        private String language;            // ISO-code, e.g. "en-US"
    }

    @Data
    public static class VoiceSettings {
        private String api_key_ref;         // secret id for ElevenLabs, etc.
    }

    @Data
    public static class MessageHistory {
        private String content;
        private String role;                // "assistant" | "user"
    }

    @Data
    public static class InterruptionSettings {
        private Integer user_response_timeout_ms;
    }
}

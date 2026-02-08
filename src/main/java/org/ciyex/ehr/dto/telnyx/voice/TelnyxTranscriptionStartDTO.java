package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxTranscriptionStartDTO {
    private String transcription_engine = "A";
    private TranscriptionEngineConfig transcription_engine_config;
    private String client_state;
    private String transcription_tracks = "inbound";
    private String command_id;

    @Data
    public static class TranscriptionEngineConfig {
        private String language = "en";
        private Boolean interim_results;
        private Boolean enable_speaker_diarization;
        private Integer min_speaker_count = 2;
        private Integer max_speaker_count = 6;
        private Boolean profanity_filter;
        private Boolean use_enhanced;
        private String model;
        private List<String> hints;
        private List<SpeechContext> speech_context;

        @Data
        public static class SpeechContext {
            private List<String> phrases;
            private Double boost = 1.0;
        }
    }
}


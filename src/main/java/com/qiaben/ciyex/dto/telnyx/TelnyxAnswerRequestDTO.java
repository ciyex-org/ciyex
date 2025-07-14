package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxAnswerRequestDTO {

    private String billing_group_id;
    private String client_state;
    private String command_id;

    private List<Header> custom_headers;
    private String preferred_codecs;
    private List<Header> sip_headers;

    private SoundModifications sound_modifications;
    private String stream_url;
    private String stream_track;
    private String stream_codec;
    private String stream_bidirectional_mode;
    private String stream_bidirectional_codec;
    private String stream_bidirectional_target_legs;
    private Boolean send_silence_when_idle;

    private String webhook_url;
    private String webhook_url_method;

    private Boolean transcription;
    private TranscriptionConfig transcription_config;

    private String record;
    private String record_channels;
    private String record_format;
    private Integer record_max_length;
    private Integer record_timeout_secs;
    private String record_track;
    private String record_trim;
    private String record_custom_file_name;

    @Data
    public static class Header {
        private String name;
        private String value;
    }

    @Data
    public static class SoundModifications {
        private Double pitch;
        private Double semitone;
        private Double octaves;
        private String track;
    }

    @Data
    public static class TranscriptionConfig {
        private String transcription_engine;
        private EngineConfig transcription_engine_config;
        private String client_state;
        private String transcription_tracks;

        @Data
        public static class EngineConfig {
            private String language;
            private Boolean interim_results;
            private Boolean enable_speaker_diarization;
            private Integer min_speaker_count;
            private Integer max_speaker_count;
            private Boolean profanity_filter;
            private Boolean use_enhanced;
            private String model;
            private List<String> hints;
        }
    }
}

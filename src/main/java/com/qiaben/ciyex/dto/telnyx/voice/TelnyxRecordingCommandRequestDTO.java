package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxRecordingCommandRequestDTO {
    private String format;
    private String channels;
    private String clientState;
    private String commandId;
    private Boolean playBeep;
    private Integer maxLength;
    private Integer timeoutSecs;
    private String recordingTrack;
    private String trim;
    private String customFileName;
    private Boolean transcription;
    private String transcriptionEngine;
    private String transcriptionLanguage;
    private Boolean transcriptionProfanityFilter;
    private Boolean transcriptionSpeakerDiarization;
    private Integer transcriptionMinSpeakerCount;
    private Integer transcriptionMaxSpeakerCount;
    private String recordingId; // required for pause/resume/stop
}

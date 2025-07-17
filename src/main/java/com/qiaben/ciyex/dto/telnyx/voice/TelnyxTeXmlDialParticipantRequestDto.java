package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlDialParticipantRequestDto {
    private String To;
    private String From;
    private String Beep;
    private String StatusCallback;
    private String StatusCallbackMethod;
    private String StatusCallbackEvent;
    private Integer Timeout;
    private Boolean Muted;
    private Boolean StartConferenceOnEnter;
    private Boolean EndConferenceOnExit;
    private Boolean EarlyMedia;
    private String ConferenceStatusCallback;
    private String ConferenceStatusCallbackMethod;
    private String ConferenceStatusCallbackEvent;
    private String WaitUrl;
    private Integer MaxParticipants;
    private Boolean Coaching;
    private String CallSidToCoach;
    private String CallerId;
    private Integer TimeLimit;
    private String MachineDetection;
    private Integer MachineDetectionTimeout;
    private Integer MachineDetectionSpeechThreshold;
    private Integer MachineDetectionSpeechEndThreshold;
    private Integer MachineDetectionSilenceTimeout;
    private String AmdStatusCallback;
    private String AmdStatusCallbackMethod;
    private Boolean CancelPlaybackOnMachineDetection;
    private Boolean CancelPlaybackOnDetectMessageEnd;
    private String PreferredCodecs;
    private Boolean Record;
    private String RecordingChannels;
    private String RecordingStatusCallback;
    private String RecordingStatusCallbackMethod;
    private String RecordingStatusCallbackEvent;
    private String RecordingTrack;
    private String SipAuthUsername;
    private String SipAuthPassword;
    private String Trim;
    private String ConferenceRecord;
    private String ConferenceRecordingStatusCallback;
    private String ConferenceRecordingStatusCallbackMethod;
    private String ConferenceRecordingStatusCallbackEvent;
    private Integer ConferenceRecordingTimeout;
    private String ConferenceTrim;
}

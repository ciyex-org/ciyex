package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class InitiateCallRequestDtoDeprecated {
    private String To;
    private String From;
    private String CallerId;
    private String Url;
    private String UrlMethod;
    private String FallbackUrl;
    private String StatusCallback;
    private String StatusCallbackMethod;
    private String StatusCallbackEvent;
    private String MachineDetection;
    private String DetectionMode;
    private Boolean AsyncAmd;
    private String AsyncAmdStatusCallback;
    private String AsyncAmdStatusCallbackMethod;
    private Integer MachineDetectionTimeout;
    private Integer MachineDetectionSpeechThreshold;
    private Integer MachineDetectionSpeechEndThreshold;
    private Integer MachineDetectionSilenceTimeout;
    private Boolean CancelPlaybackOnMachineDetection;
    private Boolean CancelPlaybackOnDetectMessageEnd;
    private String PreferredCodecs;
    private Boolean Record;
    private String RecordingChannels;
    private String RecordingStatusCallback;
    private String RecordingStatusCallbackMethod;
    private String RecordingStatusCallbackEvent;
    private Integer RecordingTimeout;
    private String RecordingTrack;
    private String SipAuthPassword;
    private String SipAuthUsername;
    private String Trim;
}


package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class JoinConferenceRequestDto {
    private String callControlId;
    private String clientState;
    private String commandId;
    private Boolean endConferenceOnExit;
    private Boolean softEndConferenceOnExit;
    private Boolean hold;
    private String holdAudioUrl;
    private String holdMediaName;
    private Boolean mute;
    private Boolean startConferenceOnEnter;
    private String supervisorRole; // barge | monitor | none | whisper
    private List<String> whisperCallControlIds;
    private String beepEnabled; // always | never | on_enter | on_exit
}

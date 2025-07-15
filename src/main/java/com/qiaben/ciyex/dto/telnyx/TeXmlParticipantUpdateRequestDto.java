package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TeXmlParticipantUpdateRequestDto {
    private Boolean Muted;
    private Boolean Hold;
    private String HoldUrl;
    private String HoldMethod;
    private String AnnounceUrl;
    private String AnnounceMethod;
    private String WaitUrl;
    private Boolean BeepOnExit;
    private Boolean EndConferenceOnExit;
    private Boolean Coaching;
    private String CallSidToCoach;
}

package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlParticipantUpdateRequestDto {
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

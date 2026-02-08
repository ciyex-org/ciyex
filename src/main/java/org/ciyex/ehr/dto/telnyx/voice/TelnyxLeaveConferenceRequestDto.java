package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxLeaveConferenceRequestDto {
    private String callControlId;
    private String commandId;
    private String beepEnabled; // always | never | on_enter | on_exit
}

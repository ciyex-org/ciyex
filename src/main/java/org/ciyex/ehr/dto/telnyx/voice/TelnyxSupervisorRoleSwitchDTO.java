package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxSupervisorRoleSwitchDTO {
    private String role; // barge, whisper, monitor
}


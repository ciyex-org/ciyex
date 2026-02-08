package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlUpdateCallRecordingRequestDto {
    private String status; // "in-progress", "paused", "stopped"
}

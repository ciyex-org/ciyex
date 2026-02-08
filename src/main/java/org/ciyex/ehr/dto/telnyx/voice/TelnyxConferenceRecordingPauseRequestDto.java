package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxConferenceRecordingPauseRequestDto {
    private String commandId;     // Optional
    private String recordingId;   // Required
}

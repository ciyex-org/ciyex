package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxConferenceRecordingStopRequestDto {
    private String clientState;    // Optional Base-64 string
    private String commandId;      // Optional
    private String recordingId;    // Required UUID string
}

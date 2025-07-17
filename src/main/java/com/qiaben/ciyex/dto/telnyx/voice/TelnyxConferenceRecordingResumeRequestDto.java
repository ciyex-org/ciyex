package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxConferenceRecordingResumeRequestDto {
    private String commandId;     // Optional
    private String recordingId;   // Required
}

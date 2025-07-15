package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ConferenceRecordingResumeRequestDto {
    private String commandId;     // Optional
    private String recordingId;   // Required
}

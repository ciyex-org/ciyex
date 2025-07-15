package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ConferenceRecordingStopRequestDto {
    private String clientState;    // Optional Base-64 string
    private String commandId;      // Optional
    private String recordingId;    // Required UUID string
}

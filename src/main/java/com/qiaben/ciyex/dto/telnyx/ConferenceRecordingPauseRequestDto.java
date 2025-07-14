package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ConferenceRecordingPauseRequestDto {
    private String commandId;     // Optional
    private String recordingId;   // Required
}

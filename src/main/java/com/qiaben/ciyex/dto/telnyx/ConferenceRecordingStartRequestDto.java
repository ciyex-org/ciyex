package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class ConferenceRecordingStartRequestDto {
    private String format;           // Required: mp3 | wav
    private String commandId;        // Optional
    private Boolean playBeep;        // Optional
    private String trim;             // Optional: trim-silence
    private String customFileName;   // Optional (<= 40 chars)
}

package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlUpdateCallRecordingRequestDto {
    private String status; // "in-progress", "paused", "stopped"
}

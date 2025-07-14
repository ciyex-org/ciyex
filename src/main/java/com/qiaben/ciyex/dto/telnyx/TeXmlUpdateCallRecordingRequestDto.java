package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TeXmlUpdateCallRecordingRequestDto {
    private String status; // "in-progress", "paused", "stopped"
}

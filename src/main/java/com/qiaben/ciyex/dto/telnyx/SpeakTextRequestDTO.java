package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class SpeakTextRequestDTO {
    private String payload;
    private String payloadType = "text";
    private String serviceLevel = "premium";
    private String stop;
    private String voice;
    private Map<String, String> voiceSettings;
    private String clientState;
    private String commandId;
}

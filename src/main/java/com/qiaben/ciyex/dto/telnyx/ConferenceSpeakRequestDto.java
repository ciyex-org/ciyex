package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ConferenceSpeakRequestDto {
    private List<String> callControlIds;          // optional – all participants if null/empty
    private String payload;                       // required – text or SSML (≤ 3 000 chars)
    private String payloadType = "text";          // text | ssml   (default text)
    private String voice;                         // required – e.g. AWS.Polly.Joanna-Neural
    private Map<String, Object> voiceSettings;    // optional – provider-specific settings
    private String commandId;                     // optional – idempotency
}

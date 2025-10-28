package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class TelehealthConfig {

    private Long orgId; // Will be set by OrgIntegrationConfigProvider
    private String vendor; // e.g., "twilio", "telnyx", "jitsi"
    private Twilio twilio;
    private Telnyx telnyx;
    private Jitsi jitsi;

    @Data
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String messagingServiceSid;

        // NEW: required for client join tokens
        private String apiKeySid;
        private String apiKeySecret;
    }

    @Data
    public static class Telnyx {
        private String apiKey;
        private String fromNumber;
    }

    @Data
    public static class Jitsi {
        private String serverUrl; // e.g., "https://meet-stg.ciyex.com"
        private String appId;     // Jitsi app ID for JWT
        private String appSecret; // Jitsi app secret for JWT
        private Boolean enableRecording;
        private Integer defaultTokenTtl; // Default TTL in seconds
    }

    // Default constructor for JSON deserialization
    public TelehealthConfig() {}
}
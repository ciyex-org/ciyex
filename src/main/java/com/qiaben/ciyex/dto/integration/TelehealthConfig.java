package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class TelehealthConfig {

    private Long orgId; // Will be set by OrgIntegrationConfigProvider
    private String vendor; // e.g., "twilio", "telnyx"
    private Twilio twilio;
    private Telnyx telnyx;

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

    // Default constructor for JSON deserialization
    public TelehealthConfig() {}
}
package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class TelehealthConfig {

    private Long orgId; // Will be set by OrgIntegrationConfigProvider
    private String vendor; // e.g., "twilio", "telnyx"
    private TwilioConfig twilio;
    private TelnyxConfig telnyx;

    @Data
    public static class TwilioConfig {
        private String accountSid;
        private String authToken;
        private String messagingServiceSid;
    }

    @Data
    public static class TelnyxConfig {
        private String apiKey;
        private String fromNumber;
    }

    // Default constructor for JSON deserialization
    public TelehealthConfig() {}
}
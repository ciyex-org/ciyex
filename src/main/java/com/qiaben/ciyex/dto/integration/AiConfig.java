package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class AiConfig {

    private Long orgId; // Will be set by OrgIntegrationConfigProvider if needed
    private String vendor; // e.g., "azure", "openai", "mock"
    private Azure azure;
    private OpenAi openai;
    private Mock mock;
    private Defaults defaults;

    @Data
    public static class Azure {
        private String endpoint;
        private String apiVersion;
        private String deployment;
        private String apiKey;
        private boolean useManagedIdentity;
        private int timeoutMs;
    }

    @Data
    public static class OpenAi {
        private String apiKey;
        private String model; // e.g., "gpt-4o"
        private String endpoint; // Optional custom endpoint
    }

    @Data
    public static class Mock {
        private String fixedResponse; // For mock responses
    }

    @Data
    public static class Defaults {
        private double temperature;
        private int maxTokens;
        private double topP;
    }

    // Default constructor for JSON deserialization
    public AiConfig() {}
}
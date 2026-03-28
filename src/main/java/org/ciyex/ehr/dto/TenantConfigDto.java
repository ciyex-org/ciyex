package org.ciyex.ehr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete tenant configuration loaded from Keycloak group attributes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigDto {
    
    private String storageType;
    private PracticeDbConfig practiceDb;
    private FhirConfig fhir;
    private StripeConfig stripe;
    private SphereConfig sphere;
    private TwilioConfig twilio;
    private SmtpConfig smtp;
    private TelehealthConfig telehealth;
    private AiConfig ai;
    private DocumentStorageConfig documentStorage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PracticeDbConfig {
        private String schema;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FhirConfig {
        private String apiUrl;
        private String clientId;
        private String tokenUrl;
        private String scope;
        private String clientSecret;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StripeConfig {
        private String apiKey;
        private String webhookSecret;
        private String publishableKey;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SphereConfig {
        private String merchantId;
        private String apiKey;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TwilioConfig {
        private String accountSid;
        private String authToken;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmtpConfig {
        private String server;
        private String username;
        private String password;
        private Integer port;
        private Boolean useTls;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelehealthConfig {
        private String vendor;
        private TwilioTelehealthConfig twilio;
        private JitsiConfig jitsi;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TwilioTelehealthConfig {
            private String accountSid;
            private String authToken;
            private String apiKeySid;
            private String apiKeySecret;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JitsiConfig {
            private String domain;
            private String appId;
            private String appSecret;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiConfig {
        private String vendor;
        private AzureAiConfig azure;
        private OpenAiConfig openai;
        private AiDefaults defaults;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AzureAiConfig {
            private String endpoint;
            private String apiVersion;
            private String deployment;
            private String apiKey;
            private Boolean useManagedIdentity;
            private Integer timeoutMs;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OpenAiConfig {
            private String apiKey;
            private String model;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AiDefaults {
            private Double temperature;
            private Integer maxTokens;
            private Double topP;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentStorageConfig {
        private S3Config s3;
        private AzureBlobConfig azureBlob;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class S3Config {
            private String bucket;
            private String accessKey;
            private String secretKey;
            private String region;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AzureBlobConfig {
            private String connectionString;
            private String containerName;
        }
    }
}

package com.qiaben.ciyex.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.TenantConfigDto;
import com.qiaben.ciyex.dto.integration.*;
import com.qiaben.ciyex.service.KeycloakOrgService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Configuration provider that loads organization integration settings from Keycloak group attributes
 * Replaces the old database-based OrgIntegrationConfigProvider
 */
@Slf4j
@Component
public class OrgIntegrationConfigProvider {

    private final KeycloakOrgService keycloakOrgService;

    public OrgIntegrationConfigProvider(KeycloakOrgService keycloakOrgService) {
        this.keycloakOrgService = keycloakOrgService;
    }

    /**
     * Get complete tenant configuration from Keycloak group attributes
     * Uses tenant name from JWT token or x-tenant-name header
     */
    public TenantConfigDto getTenantConfig() {
        try {
            String tenantName = getCurrentTenantName();
            if (tenantName == null) {
                log.warn("No tenant name in current context");
                return null;
            }
            
            return getTenantConfigForTenant(tenantName);
        } catch (Exception e) {
            log.error("Failed to get tenant config", e);
            return null;
        }
    }

    /**
     * Get complete tenant configuration for specific tenant
     * @param tenantName The tenant name (e.g., "practice_1", "hinisoft")
     */
    public TenantConfigDto getTenantConfigForTenant(String tenantName) {
        try {
            String tenantGroup = "/Tenants/" + tenantName;
            Map<String, Object> attrs = keycloakOrgService.getAllAttributes(tenantGroup);
            
            return TenantConfigDto.builder()
                    .storageType(getString(attrs, "storage_type"))
                    .practiceDb(buildPracticeDb(attrs))
                    .fhir(buildFhir(attrs))
                    .stripe(buildStripe(attrs))
                    .sphere(buildSphere(attrs))
                    .twilio(buildTwilio(attrs))
                    .smtp(buildSmtp(attrs))
                    .telehealth(buildTelehealth(attrs))
                    .ai(buildAi(attrs))
                    .documentStorage(buildDocumentStorage(attrs))
                    .build();
        } catch (Exception e) {
            log.error("Failed to get tenant config for tenant: {}", tenantName, e);
            return null;
        }
    }

    /**
     * Get current tenant name from RequestContext
     * The tenant name is set by TenantContextInterceptor from x-tenant-name header or JWT groups
     */
    private String getCurrentTenantName() {
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getTenantName() != null) {
            return ctx.getTenantName();
        }
        
        log.warn("No tenant name found in RequestContext. Ensure TenantContextInterceptor is configured.");
        return null;
    }

    private TenantConfigDto.PracticeDbConfig buildPracticeDb(Map<String, Object> attrs) {
        return TenantConfigDto.PracticeDbConfig.builder()
                .schema(getString(attrs, "practice_db_schema"))
                .build();
    }

    private TenantConfigDto.FhirConfig buildFhir(Map<String, Object> attrs) {
        return TenantConfigDto.FhirConfig.builder()
                .apiUrl(getString(attrs, "fhir_api_url"))
                .clientId(getString(attrs, "fhir_client_id"))
                .tokenUrl(getString(attrs, "fhir_token_url"))
                .scope(getString(attrs, "fhir_scope"))
                .clientSecret(getString(attrs, "fhir_client_secret"))
                .build();
    }

    private TenantConfigDto.StripeConfig buildStripe(Map<String, Object> attrs) {
        return TenantConfigDto.StripeConfig.builder()
                .apiKey(getString(attrs, "stripe_api_key"))
                .webhookSecret(getString(attrs, "stripe_webhook_secret"))
                .publishableKey(getString(attrs, "stripe_publishable_key"))
                .build();
    }

    private TenantConfigDto.SphereConfig buildSphere(Map<String, Object> attrs) {
        return TenantConfigDto.SphereConfig.builder()
                .merchantId(getString(attrs, "sphere_merchant_id"))
                .apiKey(getString(attrs, "sphere_api_key"))
                .build();
    }

    private TenantConfigDto.TwilioConfig buildTwilio(Map<String, Object> attrs) {
        return TenantConfigDto.TwilioConfig.builder()
                .accountSid(getString(attrs, "twilio_account_sid"))
                .authToken(getString(attrs, "twilio_auth_token"))
                .build();
    }

    private TenantConfigDto.SmtpConfig buildSmtp(Map<String, Object> attrs) {
        return TenantConfigDto.SmtpConfig.builder()
                .server(getString(attrs, "smtp_server"))
                .username(getString(attrs, "smtp_username"))
                .password(getString(attrs, "smtp_password"))
                .port(getInteger(attrs, "smtp_port"))
                .useTls(getBoolean(attrs, "smtp_use_tls"))
                .build();
    }

    private TenantConfigDto.TelehealthConfig buildTelehealth(Map<String, Object> attrs) {
        String vendor = getString(attrs, "telehealth_vendor");
        
        TenantConfigDto.TelehealthConfig.TwilioTelehealthConfig twilioConfig = 
                TenantConfigDto.TelehealthConfig.TwilioTelehealthConfig.builder()
                        .accountSid(getString(attrs, "telehealth_twilio_account_sid"))
                        .authToken(getString(attrs, "telehealth_twilio_auth_token"))
                        .apiKeySid(getString(attrs, "telehealth_twilio_api_key_sid"))
                        .apiKeySecret(getString(attrs, "telehealth_twilio_api_key_secret"))
                        .build();
        
        TenantConfigDto.TelehealthConfig.JitsiConfig jitsiConfig =
                TenantConfigDto.TelehealthConfig.JitsiConfig.builder()
                        .domain(getString(attrs, "telehealth_jitsi_domain"))
                        .appId(getString(attrs, "telehealth_jitsi_app_id"))
                        .appSecret(getString(attrs, "telehealth_jitsi_app_secret"))
                        .build();
        
        return TenantConfigDto.TelehealthConfig.builder()
                .vendor(vendor)
                .twilio(twilioConfig)
                .jitsi(jitsiConfig)
                .build();
    }

    private TenantConfigDto.AiConfig buildAi(Map<String, Object> attrs) {
        String vendor = getString(attrs, "ai_vendor");
        
        TenantConfigDto.AiConfig.AzureAiConfig azureConfig =
                TenantConfigDto.AiConfig.AzureAiConfig.builder()
                        .endpoint(getString(attrs, "ai_azure_endpoint"))
                        .apiVersion(getString(attrs, "ai_azure_api_version"))
                        .deployment(getString(attrs, "ai_azure_deployment"))
                        .apiKey(getString(attrs, "ai_azure_api_key"))
                        .useManagedIdentity(getBoolean(attrs, "ai_azure_use_managed_identity"))
                        .timeoutMs(getInteger(attrs, "ai_azure_timeout_ms"))
                        .build();
        
        TenantConfigDto.AiConfig.OpenAiConfig openaiConfig =
                TenantConfigDto.AiConfig.OpenAiConfig.builder()
                        .apiKey(getString(attrs, "ai_openai_api_key"))
                        .model(getString(attrs, "ai_openai_model"))
                        .build();
        
        TenantConfigDto.AiConfig.AiDefaults defaults =
                TenantConfigDto.AiConfig.AiDefaults.builder()
                        .temperature(getDouble(attrs, "ai_defaults_temperature"))
                        .maxTokens(getInteger(attrs, "ai_defaults_max_tokens"))
                        .topP(getDouble(attrs, "ai_defaults_top_p"))
                        .build();
        
        return TenantConfigDto.AiConfig.builder()
                .vendor(vendor)
                .azure(azureConfig)
                .openai(openaiConfig)
                .defaults(defaults)
                .build();
    }

    private TenantConfigDto.DocumentStorageConfig buildDocumentStorage(Map<String, Object> attrs) {
        TenantConfigDto.DocumentStorageConfig.S3Config s3Config =
                TenantConfigDto.DocumentStorageConfig.S3Config.builder()
                        .bucket(getString(attrs, "document_storage_s3_bucket"))
                        .accessKey(getString(attrs, "document_storage_s3_access_key"))
                        .secretKey(getString(attrs, "document_storage_s3_secret_key"))
                        .region(getString(attrs, "document_storage_s3_region"))
                        .build();
        
        TenantConfigDto.DocumentStorageConfig.AzureBlobConfig azureConfig =
                TenantConfigDto.DocumentStorageConfig.AzureBlobConfig.builder()
                        .connectionString(getString(attrs, "document_storage_azure_connection_string"))
                        .containerName(getString(attrs, "document_storage_azure_container_name"))
                        .build();
        
        return TenantConfigDto.DocumentStorageConfig.builder()
                .s3(s3Config)
                .azureBlob(azureConfig)
                .build();
    }

    // Helper methods
    private String getString(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDouble(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get storage type for current org from Keycloak
     */
    public String getStorageTypeForCurrentOrg() {
        try {
            String tenantGroup = getCurrentTenant();
            return keycloakOrgService.getStorageType(tenantGroup);
        } catch (Exception e) {
            log.error("Failed to get storage type for current org", e);
            return null;
        }
    }

    /**
     * Get FHIR configuration from Keycloak
     */
    public FhirConfig getFhirConfigForCurrentOrg() {
        try {
            String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            FhirConfig config = new FhirConfig();
            config.setApiUrl((String) attributes.get("fhir_api_url"));
            config.setClientId((String) attributes.get("fhir_client_id"));
            config.setClientSecret((String) attributes.get("fhir_client_secret"));
            config.setTokenUrl((String) attributes.get("fhir_token_url"));
            config.setScope((String) attributes.get("fhir_scope"));
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get FHIR config for current org", e);
            return null;
        }
    }

    /**
     * Get storage configuration from Keycloak
     */
    public StorageConfig getStorageConfigForCurrentOrg() {
        try {
            String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            StorageConfig config = new StorageConfig();
            
            // S3 Config
            StorageConfig.S3 s3 = new StorageConfig.S3();
            s3.setBucket((String) attributes.get("s3_bucket"));
            s3.setRegion((String) attributes.get("s3_region"));
            s3.setAccessKey((String) attributes.get("s3_access_key"));
            s3.setSecretKey((String) attributes.get("s3_secret_key"));
            config.setS3(s3);
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get storage config for current org", e);
            return null;
        }
    }

    /**
     * Get telehealth configuration from Keycloak
     */
    public TelehealthConfig getTelehealthConfigForCurrentOrg() {
        try {
            String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            TelehealthConfig config = new TelehealthConfig();
            //config.setOrgId(orgId);
            config.setVendor((String) attributes.get("telehealth_vendor"));
            
            // Telnyx Config
            if ("telnyx".equalsIgnoreCase(config.getVendor())) {
                TelehealthConfig.Telnyx telnyx = new TelehealthConfig.Telnyx();
                telnyx.setApiKey((String) attributes.get("telnyx_api_key"));
                telnyx.setFromNumber((String) attributes.get("telnyx_from_number"));
                config.setTelnyx(telnyx);
            }
            
            // Twilio Config
            if ("twilio".equalsIgnoreCase(config.getVendor())) {
                TelehealthConfig.Twilio twilio = new TelehealthConfig.Twilio();
                twilio.setAccountSid((String) attributes.get("twilio_account_sid"));
                twilio.setAuthToken((String) attributes.get("twilio_auth_token"));
                twilio.setApiKeySid((String) attributes.get("twilio_api_key_sid"));
                twilio.setApiKeySecret((String) attributes.get("twilio_api_key_secret"));
                twilio.setMessagingServiceSid((String) attributes.get("twilio_messaging_service_sid"));
                config.setTwilio(twilio);
            }
            
            // Jitsi Config
            if ("jitsi".equalsIgnoreCase(config.getVendor())) {
                TelehealthConfig.Jitsi jitsi = new TelehealthConfig.Jitsi();
                jitsi.setServerUrl((String) attributes.get("jitsi_server_url"));
                jitsi.setAppId((String) attributes.get("jitsi_app_id"));
                jitsi.setAppSecret((String) attributes.get("jitsi_app_secret"));
                
                String enableRecording = (String) attributes.get("jitsi_enable_recording");
                if (enableRecording != null) {
                    jitsi.setEnableRecording(Boolean.parseBoolean(enableRecording));
                }
                
                String ttl = (String) attributes.get("jitsi_default_token_ttl");
                if (ttl != null) {
                    jitsi.setDefaultTokenTtl(Integer.parseInt(ttl));
                }
                
                config.setJitsi(jitsi);
            }
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get telehealth config for current org", e);
            return null;
        }
    }

    /**
     * Get AI configuration from Keycloak
     */
    public AiConfig getAiConfigForCurrentOrg() {
        try {
            String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            AiConfig config = new AiConfig();
            //config.setOrgId(orgId);
            config.setVendor((String) attributes.get("ai_vendor"));
            
            // Azure Config
            if ("azure".equalsIgnoreCase(config.getVendor())) {
                AiConfig.Azure azure = new AiConfig.Azure();
                azure.setEndpoint((String) attributes.get("azure_endpoint"));
                azure.setApiKey((String) attributes.get("azure_api_key"));
                azure.setDeployment((String) attributes.get("azure_deployment"));
                azure.setApiVersion((String) attributes.get("azure_api_version"));
                
                String useManagedIdentity = (String) attributes.get("azure_use_managed_identity");
                if (useManagedIdentity != null) {
                    azure.setUseManagedIdentity(Boolean.parseBoolean(useManagedIdentity));
                }
                
                String timeout = (String) attributes.get("azure_timeout_ms");
                if (timeout != null) {
                    azure.setTimeoutMs(Integer.parseInt(timeout));
                }
                
                config.setAzure(azure);
            }
            
            // OpenAI Config
            if ("openai".equalsIgnoreCase(config.getVendor())) {
                AiConfig.OpenAi openai = new AiConfig.OpenAi();
                openai.setApiKey((String) attributes.get("openai_api_key"));
                openai.setModel((String) attributes.get("openai_model"));
                openai.setEndpoint((String) attributes.get("openai_endpoint"));
                config.setOpenai(openai);
            }
            
            // Mock Config
            if ("mock".equalsIgnoreCase(config.getVendor())) {
                AiConfig.Mock mock = new AiConfig.Mock();
                mock.setFixedResponse((String) attributes.get("mock_fixed_response"));
                config.setMock(mock);
            }
            
            // Defaults
            AiConfig.Defaults defaults = new AiConfig.Defaults();
            String temperature = (String) attributes.get("ai_default_temperature");
            if (temperature != null) {
                defaults.setTemperature(Double.parseDouble(temperature));
            }
            String maxTokens = (String) attributes.get("ai_default_max_tokens");
            if (maxTokens != null) {
                defaults.setMaxTokens(Integer.parseInt(maxTokens));
            }
            String topP = (String) attributes.get("ai_default_top_p");
            if (topP != null) {
                defaults.setTopP(Double.parseDouble(topP));
            }
            config.setDefaults(defaults);
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get AI config for current org", e);
            return null;
        }
    }

    /**
     * Get GPS configuration from Keycloak (GPS payment gateway, not maps)
     */
    public GpsConfig getGpsConfigForCurrentOrg() {
        try {
String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            GpsConfig config = new GpsConfig();
            config.setUsername((String) attributes.get("gps_username"));
            config.setPassword((String) attributes.get("gps_password"));
            config.setSecurityKey((String) attributes.get("gps_security_key"));
            config.setCollectjsPublicKey((String) attributes.get("gps_collectjs_public_key"));
            config.setTransactUrl((String) attributes.get("gps_transact_url"));
            config.setWebhookUrl((String) attributes.get("gps_webhook_url"));
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get GPS config for current org", e);
            return null;
        }
    }

    /**
     * Get SMS configuration from Keycloak
     */
    public TwilioConfig getTwilioConfigForCurrentOrg() {
        try {
String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            TwilioConfig config = new TwilioConfig();
            config.setAccountSid((String) attributes.get("twilio_account_sid"));
            config.setAuthToken((String) attributes.get("twilio_auth_token"));
            config.setPhoneNumber((String) attributes.get("twilio_phone_number"));
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get Twilio config for current org", e);
            return null;
        }
    }

    /**
     * Generic method to get configuration by integration key
     */
    public <T> T get(String tenantGroup, IntegrationKey key) {
        switch (key) {
            case FHIR:
                return (T) getFhirConfig(tenantGroup);
            case DOCUMENT_STORAGE:
                return (T) getStorageConfig(tenantGroup);
            case TELEHEALTH:
                return (T) getTelehealthConfig(tenantGroup);
            case AI:
                return (T) getAiConfig(tenantGroup);
            case GPS:
                return (T) getGpsConfig(tenantGroup);
            case TWILIO:
                return (T) getTwilioConfig(tenantGroup);
            default:
                return null;
        }
    }
    
    /**
     * Get configuration for current org by integration key
     */
    public <T> T getForCurrentTenant(IntegrationKey key) {
        return get(getCurrentTenant(), key);
    }
    
    /**
     * Get GPS config for current org (backward compatibility)
     */
    public GpsConfig getGpsForCurrentOrg() {
        return getGpsConfigForCurrentOrg();
    }
    
    /**
     * Get Stripe config for current org (backward compatibility)
     */
    public Map<String, Object> getStripeForCurrentOrg() {
        try {
String tenantGroup = getCurrentTenant();
            return keycloakOrgService.getAllAttributes(tenantGroup);
        } catch (Exception e) {
            log.error("Failed to get Stripe config for current org", e);
            return null;
        }
    }
    
    /**
     * Get S3 document storage config
     */
    public S3Config getS3DocumentStorage() {
        try {
            String tenantGroup = getCurrentTenant();
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            S3Config config = new S3Config();
            config.setBucketName((String) attributes.get("s3_bucket_name"));
            config.setRegion((String) attributes.get("s3_region"));
            config.setAccessKeyId((String) attributes.get("s3_access_key_id"));
            config.setSecretAccessKey((String) attributes.get("s3_secret_access_key"));
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get S3 document storage config", e);
            return null;
        }
    }

    // Private helper methods for get() method
    
    private FhirConfig getFhirConfig(String tenantGroup) {
        try {
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            FhirConfig config = new FhirConfig();
            config.setApiUrl((String) attributes.get("fhir_api_url"));
            config.setClientId((String) attributes.get("fhir_client_id"));
            config.setClientSecret((String) attributes.get("fhir_client_secret"));
            config.setTokenUrl((String) attributes.get("fhir_token_url"));
            config.setScope((String) attributes.get("fhir_scope"));
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get FHIR config", e);
            return null;
        }
    }
    
    private StorageConfig getStorageConfig(String tenantGroup) {
        try {
            Map<String, Object> attributes = keycloakOrgService.getAllAttributes(tenantGroup);
            
            StorageConfig config = new StorageConfig();
            
            StorageConfig.S3 s3 = new StorageConfig.S3();
            s3.setBucket((String) attributes.get("s3_bucket"));
            s3.setRegion((String) attributes.get("s3_region"));
            s3.setAccessKey((String) attributes.get("s3_access_key"));
            s3.setSecretKey((String) attributes.get("s3_secret_key"));
            config.setS3(s3);
            
            return config;
        } catch (Exception e) {
            log.error("Failed to get storage config", e);
            return null;
        }
    }
    
    private TelehealthConfig getTelehealthConfig(String tenantGroup) {
        return getTelehealthConfigForCurrentOrg(); // Reuse existing method
    }
    
    private AiConfig getAiConfig(String tenantGroup) {
        return getAiConfigForCurrentOrg(); // Reuse existing method
    }
    
    private GpsConfig getGpsConfig(String tenantGroup) {
        return getGpsConfigForCurrentOrg(); // Reuse existing method
    }
    
    private TwilioConfig getTwilioConfig(String tenantGroup) {
        return getTwilioConfigForCurrentOrg(); // Reuse existing method
    }
    
    // Helper methods
    
    private String getCurrentTenant() {
        RequestContext context = RequestContext.get();
        return context != null ? context.getTenantName() : null;
    }

    // Inner classes for backward compatibility
    
    @Data
    public static class S3Config {
        private String bucketName;
        private String region;
        private String accessKeyId;
        private String secretAccessKey;
        
        // Backward compatibility aliases
        public String getBucket() {
            return bucketName;
        }
        
        public String getAccessKey() {
            return accessKeyId;
        }
        
        public String getSecretKey() {
            return secretAccessKey;
        }
    }
}

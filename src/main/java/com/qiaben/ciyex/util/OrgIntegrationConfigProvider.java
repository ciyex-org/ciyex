package com.qiaben.ciyex.util;

import com.qiaben.ciyex.dto.integration.*;
import com.qiaben.ciyex.service.OrgConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OrgIntegrationConfigProvider {

    private final OrgConfigService orgConfigService;

    public OrgIntegrationConfigProvider(OrgConfigService orgConfigService) {
        this.orgConfigService = orgConfigService;
    }

    private Map<String,String> loadAll() {
        return orgConfigService.getAllConfigsAsMap();
    }

    public String getRaw(String key) { return loadAll().get(key); }

    public String getStorageType() {
        Map<String,String> all = loadAll();
        String explicit = all.get("storage_type");
        if (explicit != null && !explicit.isBlank()) return explicit;
        if (all.keySet().stream().anyMatch(k -> k.startsWith("fhir_"))) return "fhir";
        if (all.keySet().stream().anyMatch(k -> k.startsWith("practice_db_"))) return "practice_db";
        return "fhir"; // default to fhir
    }

    
    public FhirConfig getFhirConfig() {
        Map<String,String> all = loadAll();
        if (all.keySet().stream().noneMatch(k -> k.startsWith("fhir_"))) return null;
        FhirConfig c = new FhirConfig();
        c.setApiUrl(all.get("fhir_api_url"));
        c.setClientId(all.get("fhir_client_id"));
        c.setClientSecret(all.get("fhir_client_secret"));
        c.setTokenUrl(all.get("fhir_token_url"));
        c.setScope(all.get("fhir_scope"));
        return c;
    }

    
    public StripeConfig getStripeConfig() {
        Map<String,String> all = loadAll();
        if (all.keySet().stream().noneMatch(k -> k.startsWith("stripe_"))) return null;
        StripeConfig c = new StripeConfig();
        c.setApiKey(all.get("stripe_api_key"));
        c.setWebhookSecret(all.get("stripe_webhook_secret"));
        c.setPublishableKey(all.get("stripe_publishable_key"));
        return c;
    }

    
    public TwilioConfig getTwilioConfig() {
        Map<String,String> all = loadAll();
        if (all.keySet().stream().noneMatch(k -> k.startsWith("twilio_"))) return null;
        TwilioConfig c = new TwilioConfig();
        c.setAccountSid(all.get("twilio_account_sid"));
        c.setAuthToken(all.get("twilio_auth_token"));
        c.setPhoneNumber(all.get("twilio_phone_number"));
        return c;
    }

    
    public GpsConfig getGpsConfig() {
        Map<String,String> all = loadAll();
        if (all.keySet().stream().noneMatch(k -> k.startsWith("gps_"))) return null;
        GpsConfig c = new GpsConfig();
        c.setUsername(all.get("gps_username"));
        c.setPassword(all.get("gps_password"));
        c.setSecurityKey(all.get("gps_security_key"));
        c.setCollectjsPublicKey(all.get("gps_collectjs_public_key"));
        c.setTransactUrl(all.get("gps_transact_url"));
        c.setWebhookUrl(all.get("gps_webhook_url"));
        return c;
    }

    
    public AiConfig getAiConfig() {
        Map<String,String> all = loadAll();
        if (all.keySet().stream().noneMatch(k -> k.startsWith("ai_"))) return null;
        AiConfig cfg = new AiConfig();
        cfg.setVendor(all.get("ai_vendor"));
        if ("azure".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.Azure azure = new AiConfig.Azure();
            azure.setEndpoint(all.get("ai_azure_endpoint"));
            azure.setApiKey(all.get("ai_azure_api_key"));
            azure.setDeployment(all.get("ai_azure_deployment"));
            azure.setApiVersion(all.get("ai_azure_api_version"));
            String useMI = all.get("ai_azure_use_managed_identity");
            if (useMI != null) azure.setUseManagedIdentity(Boolean.parseBoolean(useMI));
            String timeout = all.get("ai_azure_timeout_ms");
            if (timeout != null) try { azure.setTimeoutMs(Integer.parseInt(timeout)); } catch (NumberFormatException ignored) {}
            cfg.setAzure(azure);
        }
        if ("openai".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.OpenAi openai = new AiConfig.OpenAi();
            openai.setApiKey(all.get("ai_openai_api_key"));
            openai.setModel(all.get("ai_openai_model"));
            openai.setEndpoint(all.get("ai_openai_endpoint"));
            cfg.setOpenai(openai);
        }
        if ("mock".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.Mock mock = new AiConfig.Mock();
            mock.setFixedResponse(all.get("ai_mock_fixed_response"));
            cfg.setMock(mock);
        }
        AiConfig.Defaults d = new AiConfig.Defaults();
        String t = all.get("ai_default_temperature"); if (t!=null) try { d.setTemperature(Double.parseDouble(t)); } catch (NumberFormatException ignored) {}
        String m = all.get("ai_default_max_tokens"); if (m!=null) try { d.setMaxTokens(Integer.parseInt(m)); } catch (NumberFormatException ignored) {}
        String p = all.get("ai_default_top_p"); if (p!=null) try { d.setTopP(Double.parseDouble(p)); } catch (NumberFormatException ignored) {}
        cfg.setDefaults(d);
        return cfg;
    }

    
    public StorageConfig getDocumentStorageConfig() {
        Map<String,String> all = loadAll();

        // Check for new dot notation format first
        boolean hasS3Dots = all.containsKey("document_storage.s3.bucket")
                || all.containsKey("document_storage.s3.region")
                || all.containsKey("document_storage.s3.accessKey")
                || all.containsKey("document_storage.s3.secretKey");

        // Fallback to old underscore format
        boolean hasS3Underscore = all.keySet().stream().anyMatch(k -> k.startsWith("document_storage_s3_"));

        if (!hasS3Dots && !hasS3Underscore) return null;

        StorageConfig sc = new StorageConfig();
        StorageConfig.S3 s3 = new StorageConfig.S3();

        if (hasS3Dots) {
            // New dot notation format
            s3.setBucket(all.get("document_storage.s3.bucket"));
            s3.setRegion(all.get("document_storage.s3.region"));
            s3.setAccessKey(all.get("document_storage.s3.accessKey"));
            s3.setSecretKey(all.get("document_storage.s3.secretKey"));
        } else {
            // Old underscore format (backward compatibility)
            s3.setBucket(all.get("document_storage_s3_bucket"));
            s3.setRegion(all.get("document_storage_s3_region"));
            s3.setAccessKey(all.get("document_storage_s3_access_key"));
            s3.setSecretKey(all.get("document_storage_s3_secret_key"));
        }

        sc.setS3(s3);
        return sc;
    }

    @SuppressWarnings("unchecked")
    
    public <T> T get(IntegrationKey key) {
        return switch (key) {
            case FHIR -> (T) getFhirConfig();
            case STRIPE -> (T) getStripeConfig();
            case TWILIO -> (T) getTwilioConfig();
            case GPS -> (T) getGpsConfig();
            case AI -> (T) getAiConfig();
            case DOCUMENT_STORAGE -> (T) getDocumentStorageConfig();
            case PRACTICE_DB, SPHERE, SMTP, TELEHEALTH -> null; // not implemented
        };
    }

    // Alias for backward compatibility
    @SuppressWarnings("unchecked")
    
    public <T> T getForCurrentTenant(IntegrationKey key) {
        return get(key);
    }

    // Shortcut methods for backward compatibility
    public StripeConfig getStripeForCurrentOrg() {
        return getStripeConfig();
    }

    public GpsConfig getGpsForCurrentOrg() {
        return getGpsConfig();
    }

    public String getStorageTypeForCurrentOrg() {
        return getStorageType();
    }

    public StorageConfig getS3DocumentStorageForCurrentOrg() {
        return getDocumentStorageConfig();
    }

    // Return S3 config directly for DocumentService compatibility
    public StorageConfig.S3 getS3DocumentStorage() {
        StorageConfig cfg = getDocumentStorageConfig();
        return cfg != null ? cfg.getS3() : null;
    }

    // Placeholder for IntegrationConfigController compatibility
    public TenantConfigDto getTenantConfig() {
        log.warn("getTenantConfig() called - this is deprecated, returning empty DTO");
        return new TenantConfigDto();
    }

    // Inner DTO for deprecated getTenantConfig method
    public static class TenantConfigDto {
        // Empty placeholder - old callers should migrate to specific config getters
    }
}
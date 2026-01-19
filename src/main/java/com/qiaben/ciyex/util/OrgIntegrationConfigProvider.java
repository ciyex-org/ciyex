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
        boolean hasDot = all.containsKey("fhir.apiUrl") || all.containsKey("fhir.clientId");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("fhir_"));
        if (!hasDot && !hasUnderscore) return null;
        FhirConfig c = new FhirConfig();
        c.setApiUrl(hasDot ? all.get("fhir.apiUrl") : all.get("fhir_api_url"));
        c.setClientId(hasDot ? all.get("fhir.clientId") : all.get("fhir_client_id"));
        c.setClientSecret(hasDot ? all.get("fhir.clientSecret") : all.get("fhir_client_secret"));
        c.setTokenUrl(hasDot ? all.get("fhir.tokenUrl") : all.get("fhir_token_url"));
        c.setScope(hasDot ? all.get("fhir.scope") : all.get("fhir_scope"));
        return c;
    }

    
    public StripeConfig getStripeConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("stripe.apiKey") || all.containsKey("stripe.webhookSecret");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("stripe_"));
        if (!hasDot && !hasUnderscore) return null;
        StripeConfig c = new StripeConfig();
        c.setApiKey(hasDot ? all.get("stripe.apiKey") : all.get("stripe_api_key"));
        c.setWebhookSecret(hasDot ? all.get("stripe.webhookSecret") : all.get("stripe_webhook_secret"));
        c.setPublishableKey(hasDot ? all.get("stripe.publishableKey") : all.get("stripe_publishable_key"));
        return c;
    }

    
    public TwilioConfig getTwilioConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("twilio.accountSid") || all.containsKey("twilio.authToken");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("twilio_"));
        if (!hasDot && !hasUnderscore) return null;
        TwilioConfig c = new TwilioConfig();
        c.setAccountSid(hasDot ? all.get("twilio.accountSid") : all.get("twilio_account_sid"));
        c.setAuthToken(hasDot ? all.get("twilio.authToken") : all.get("twilio_auth_token"));
        c.setPhoneNumber(hasDot ? all.get("twilio.phoneNumber") : all.get("twilio_phone_number"));
        return c;
    }

    
    public GpsConfig getGpsConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("gps.securityKey") || all.containsKey("gps.transactUrl");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("gps_"));
        if (!hasDot && !hasUnderscore) return null;
        GpsConfig c = new GpsConfig();
        c.setUsername(hasDot ? all.get("gps.username") : all.get("gps_username"));
        c.setPassword(hasDot ? all.get("gps.password") : all.get("gps_password"));
        c.setSecurityKey(hasDot ? all.get("gps.securityKey") : all.get("gps_security_key"));
        c.setCollectjsPublicKey(hasDot ? all.get("gps.collectjsPublicKey") : all.get("gps_collectjs_public_key"));
        c.setTransactUrl(hasDot ? all.get("gps.transactUrl") : all.get("gps_transact_url"));
        c.setWebhookUrl(hasDot ? all.get("gps.webhookUrl") : all.get("gps_webhook_url"));
        return c;
    }

    
    public AiConfig getAiConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("ai.vendor") || all.containsKey("ai.azure.endpoint");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("ai_"));
        if (!hasDot && !hasUnderscore) return null;
        AiConfig cfg = new AiConfig();
        cfg.setVendor(hasDot ? all.get("ai.vendor") : all.get("ai_vendor"));
        if ("azure".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.Azure azure = new AiConfig.Azure();
            azure.setEndpoint(hasDot ? all.get("ai.azure.endpoint") : all.get("ai_azure_endpoint"));
            azure.setApiKey(hasDot ? all.get("ai.azure.apiKey") : all.get("ai_azure_api_key"));
            azure.setDeployment(hasDot ? all.get("ai.azure.deployment") : all.get("ai_azure_deployment"));
            azure.setApiVersion(hasDot ? all.get("ai.azure.apiVersion") : all.get("ai_azure_api_version"));
            String useMI = hasDot ? all.get("ai.azure.useManagedIdentity") : all.get("ai_azure_use_managed_identity");
            if (useMI != null) azure.setUseManagedIdentity(Boolean.parseBoolean(useMI));
            String timeout = hasDot ? all.get("ai.azure.timeoutMs") : all.get("ai_azure_timeout_ms");
            if (timeout != null) try { azure.setTimeoutMs(Integer.parseInt(timeout)); } catch (NumberFormatException ignored) {}
            cfg.setAzure(azure);
        }
        if ("openai".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.OpenAi openai = new AiConfig.OpenAi();
            openai.setApiKey(hasDot ? all.get("ai.openai.apiKey") : all.get("ai_openai_api_key"));
            openai.setModel(hasDot ? all.get("ai.openai.model") : all.get("ai_openai_model"));
            openai.setEndpoint(hasDot ? all.get("ai.openai.endpoint") : all.get("ai_openai_endpoint"));
            cfg.setOpenai(openai);
        }
        if ("mock".equalsIgnoreCase(cfg.getVendor())) {
            AiConfig.Mock mock = new AiConfig.Mock();
            mock.setFixedResponse(hasDot ? all.get("ai.mock.fixedResponse") : all.get("ai_mock_fixed_response"));
            cfg.setMock(mock);
        }
        AiConfig.Defaults d = new AiConfig.Defaults();
        String t = hasDot ? all.get("ai.defaults.temperature") : all.get("ai_default_temperature"); 
        if (t!=null) try { d.setTemperature(Double.parseDouble(t)); } catch (NumberFormatException ignored) {}
        String m = hasDot ? all.get("ai.defaults.maxTokens") : all.get("ai_default_max_tokens"); 
        if (m!=null) try { d.setMaxTokens(Integer.parseInt(m)); } catch (NumberFormatException ignored) {}
        String p = hasDot ? all.get("ai.defaults.topP") : all.get("ai_default_top_p"); 
        if (p!=null) try { d.setTopP(Double.parseDouble(p)); } catch (NumberFormatException ignored) {}
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

    public SmtpConfig getSmtpConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("smtp.server") || all.containsKey("smtp.username");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("smtp_"));
        if (!hasDot && !hasUnderscore) return null;
        SmtpConfig c = new SmtpConfig();
        c.setServer(hasDot ? all.get("smtp.server") : all.get("smtp_server"));
        c.setUsername(hasDot ? all.get("smtp.username") : all.get("smtp_username"));
        c.setPassword(hasDot ? all.get("smtp.password") : all.get("smtp_password"));
        c.setFromAddress(hasDot ? all.get("smtp.fromAddress") : all.get("smtp_from_address"));
        c.setFromName(hasDot ? all.get("smtp.fromName") : all.get("smtp_from_name"));
        String port = hasDot ? all.get("smtp.port") : all.get("smtp_port");
        if (port != null) try { c.setPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
        return c;
    }

    public TelehealthConfig getTelehealthConfig() {
        Map<String,String> all = loadAll();
        boolean hasDot = all.containsKey("telehealth.vendor") || all.containsKey("telehealth.twilio.accountSid");
        boolean hasUnderscore = all.keySet().stream().anyMatch(k -> k.startsWith("telehealth_"));
        if (!hasDot && !hasUnderscore) return null;
        TelehealthConfig cfg = new TelehealthConfig();
        cfg.setVendor(hasDot ? all.get("telehealth.vendor") : all.get("telehealth_vendor"));
        if ("twilio".equalsIgnoreCase(cfg.getVendor())) {
            TelehealthConfig.Twilio twilio = new TelehealthConfig.Twilio();
            twilio.setAccountSid(hasDot ? all.get("telehealth.twilio.accountSid") : all.get("telehealth_twilio_account_sid"));
            twilio.setAuthToken(hasDot ? all.get("telehealth.twilio.authToken") : all.get("telehealth_twilio_auth_token"));
            twilio.setApiKeySid(hasDot ? all.get("telehealth.twilio.apiKeySid") : all.get("telehealth_twilio_api_key_sid"));
            twilio.setApiKeySecret(hasDot ? all.get("telehealth.twilio.apiKeySecret") : all.get("telehealth_twilio_api_key_secret"));
            twilio.setMessagingServiceSid(hasDot ? all.get("telehealth.twilio.messagingServiceSid") : all.get("telehealth_twilio_messaging_service_sid"));
            cfg.setTwilio(twilio);
        }
        return cfg;
    }

    public StorageConfig.S3 getS3DocumentStorage() {
        StorageConfig cfg = getDocumentStorageConfig();
        return cfg != null ? cfg.getS3() : null;
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
            case SMTP -> (T) getSmtpConfig();
            case TELEHEALTH -> (T) getTelehealthConfig();
            case PRACTICE_DB, SPHERE -> null;
        };
    }

    @SuppressWarnings("unchecked")
    public <T> T getForCurrentTenant(IntegrationKey key) {
        return get(key);
    }

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

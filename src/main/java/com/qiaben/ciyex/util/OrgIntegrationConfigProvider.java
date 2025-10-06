package com.qiaben.ciyex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StripeConfig;
import com.qiaben.ciyex.dto.integration.GpsConfig;   // ✅ Added import
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@Slf4j
public class OrgIntegrationConfigProvider {

    private final ObjectMapper objectMapper;
    private final OrgConfigRepository orgConfigRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    @Autowired
    public OrgIntegrationConfigProvider(ObjectMapper objectMapper, OrgConfigRepository orgConfigRepository, DataSource dataSource) {
        this.objectMapper = objectMapper;
        this.orgConfigRepository = orgConfigRepository;
        this.dataSource = dataSource;
    }

    /**
     * Ensures we're in the master schema (public) before executing OrgConfig operations
     */
    private void ensureTenantSchema(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                log.debug("Set search_path to {} via DataSource for OrgIntegrationConfigProvider", schemaName);
                return;
            } catch (Exception e) {
                log.warn("Failed to set search_path via DataSource for {}: {}", schemaName, e.getMessage());
            }
        }

        try {
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public").executeUpdate();
            log.debug("Set search_path to {} via EntityManager (fallback)", schemaName);
        } catch (Exception e) {
            log.warn("Failed to set schema to {} for OrgIntegrationConfigProvider: {}", schemaName, e.getMessage());
        }
    }

    private void ensureTenantSchemaFromContext() {
        try {
            RequestContext ctx = RequestContext.get();
            if (ctx != null && ctx.getOrgId() != null) {
                ensureTenantSchema(ctx.getOrgId());
            }
        } catch (Exception e) {
            log.debug("No RequestContext available to set tenant schema: {}", e.getMessage());
        }
    }

    // Core method: Always use IntegrationKey (which knows both key and class)
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> T get(Long orgId, IntegrationKey integrationKey) {
    // Ensure we're in the tenant schema for this org
    ensureTenantSchema(orgId);
        
        // Ensure RequestContext carries the target orgId for the duration of the lookup
        RequestContext previousContext = RequestContext.get();
        boolean contextAdjusted = false;
        if (previousContext == null || !Objects.equals(previousContext.getOrgId(), orgId)) {
            RequestContext context = new RequestContext();
            if (previousContext != null) {
                context.setAuthToken(previousContext.getAuthToken());
                context.setFacilityId(previousContext.getFacilityId());
                context.setRole(previousContext.getRole());
            }
            context.setOrgId(orgId);
            RequestContext.set(context);
            contextAdjusted = true;
        }
        try {
            OrgConfig orgConfig = orgConfigRepository.findByOrgId(orgId)
                    .orElseThrow(() -> new RuntimeException("OrgConfig not found for orgId: " + orgId));
            if (orgConfig.getIntegrations() == null) return null;
            JsonNode section = orgConfig.getIntegrations().get(integrationKey.key());
            if (section == null || section.isNull()) return null;
            try {
                return (T) objectMapper.treeToValue(section, integrationKey.clazz());
            } catch (Exception e) {
                throw new RuntimeException("Failed to map integration config for " + integrationKey.key(), e);
            }
        } finally {
            if (contextAdjusted) {
                if (previousContext != null) {
                    RequestContext.set(previousContext);
                } else {
                    RequestContext.clear();
                }
            }
        }
    }

    public <T> T getForCurrentOrg(IntegrationKey integrationKey) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return get(orgId, integrationKey);
    }

    /** ✅ Shortcut for Stripe */
    public StripeConfig getStripeForCurrentOrg() {
        return getForCurrentOrg(IntegrationKey.STRIPE);
    }

    /** ✅ Shortcut for GPS */
    public GpsConfig getGpsForCurrentOrg() {
        return getForCurrentOrg(IntegrationKey.GPS);
    }

    @Transactional
    public String getStorageType(Long orgId) {
    // Ensure we're in the tenant schema for this org
    ensureTenantSchema(orgId);
        
        // Ensure RequestContext carries the target orgId for the duration of the lookup
        RequestContext previousContext = RequestContext.get();
        boolean contextAdjusted = false;
        if (previousContext == null || !Objects.equals(previousContext.getOrgId(), orgId)) {
            RequestContext context = new RequestContext();
            if (previousContext != null) {
                context.setAuthToken(previousContext.getAuthToken());
                context.setFacilityId(previousContext.getFacilityId());
                context.setRole(previousContext.getRole());
            }
            context.setOrgId(orgId);
            RequestContext.set(context);
            contextAdjusted = true;
        }
        try {
            OrgConfig orgConfig = orgConfigRepository.findByOrgId(orgId)
                    .orElseThrow(() -> new RuntimeException("OrgConfig not found for orgId: " + orgId));
            if (orgConfig.getIntegrations() == null) return null;
            JsonNode integrations = orgConfig.getIntegrations();

            // Check for explicit 'storage_type' field
            if (integrations.has("storage_type")) {
                return integrations.get("storage_type").asText(null);
            }

            // Infer based on present integration keys (prioritize FHIR, then PRACTICE_DB, etc.)
            if (integrations.has(IntegrationKey.FHIR.key())) {
                return "fhir";
            } else if (integrations.has(IntegrationKey.PRACTICE_DB.key())) {
                return "practice_db";
            } // Add more inferences as needed for other potential storage types

            return null; // No storage type detected
        } finally {
            if (contextAdjusted) {
                if (previousContext != null) {
                    RequestContext.set(previousContext);
                } else {
                    RequestContext.clear();
                }
            }
        }
    }

    public String getStorageTypeForCurrentOrg() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return getStorageType(orgId);
    }

    /** ✅ NEW: Helper for S3 Document Storage */
    @Transactional
    public S3Config getS3DocumentStorage(Long orgId) {
    // Ensure we're in the tenant schema for this org
    ensureTenantSchema(orgId);
        
        // Ensure RequestContext carries the target orgId for the duration of the lookup
        RequestContext previousContext = RequestContext.get();
        boolean contextAdjusted = false;
        if (previousContext == null || !Objects.equals(previousContext.getOrgId(), orgId)) {
            RequestContext context = new RequestContext();
            if (previousContext != null) {
                context.setAuthToken(previousContext.getAuthToken());
                context.setFacilityId(previousContext.getFacilityId());
                context.setRole(previousContext.getRole());
            }
            context.setOrgId(orgId);
            RequestContext.set(context);
            contextAdjusted = true;
        }
        try {
            OrgConfig orgConfig = orgConfigRepository.findByOrgId(orgId)
                    .orElseThrow(() -> new RuntimeException("OrgConfig not found for orgId: " + orgId));

            JsonNode integrations = orgConfig.getIntegrations();
            if (integrations == null
                    || !integrations.has("document_storage")
                    || !integrations.get("document_storage").has("s3")) {
                throw new RuntimeException("No S3 document storage config found for orgId=" + orgId);
            }

            JsonNode s3Node = integrations.get("document_storage").get("s3");
            try {
                return objectMapper.treeToValue(s3Node, S3Config.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map S3 config for orgId=" + orgId, e);
            }
        } finally {
            if (contextAdjusted) {
                if (previousContext != null) {
                    RequestContext.set(previousContext);
                } else {
                    RequestContext.clear();
                }
            }
        }
    }

    public S3Config getS3DocumentStorageForCurrentOrg() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return getS3DocumentStorage(orgId);
    }

    public static class S3Config {
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}

/*

@Autowired
private OrgIntegrationConfigProvider integrationConfigProvider;

public void someServiceMethod(Long orgId) {
    OpenEmrConfig openEmrConfig = integrationConfigProvider.get(orgId, IntegrationKey.OPENEMR);
    StripeConfig stripeConfig = integrationConfigProvider.get(orgId, IntegrationKey.STRIPE);
    TwilioConfig twilioConfig = integrationConfigProvider.get(orgId, IntegrationKey.TWILIO);
    GpsConfig gpsConfig = integrationConfigProvider.get(orgId, IntegrationKey.GPS); // ✅ GPS
    // ...and so on

    String storageType = integrationConfigProvider.getStorageType(orgId);

    S3Config s3Config = integrationConfigProvider.getS3DocumentStorage(orgId);
    // Or from context:
    // S3Config s3Config = integrationConfigProvider.getS3DocumentStorageForCurrentOrg();
    // Use storageType and s3Config to resolve the appropriate ExternalOrgStorage
}*/


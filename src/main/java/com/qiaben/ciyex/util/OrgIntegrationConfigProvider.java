package com.qiaben.ciyex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrgIntegrationConfigProvider {

    private final ObjectMapper objectMapper;
    private final OrgConfigRepository orgConfigRepository;

    @Autowired
    public OrgIntegrationConfigProvider(ObjectMapper objectMapper, OrgConfigRepository orgConfigRepository) {
        this.objectMapper = objectMapper;
        this.orgConfigRepository = orgConfigRepository;
    }

    // Core method: Always use IntegrationKey (which knows both key and class)
    @SuppressWarnings("unchecked")
    public <T> T get(Long orgId, IntegrationKey integrationKey) {
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
    }

    public <T> T getForCurrentOrg(IntegrationKey integrationKey) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return get(orgId, integrationKey);
    }

    public String getStorageType(Long orgId) {
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
    }

    public String getStorageTypeForCurrentOrg() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return getStorageType(orgId);
    }

    // ✅ NEW: Helper for S3 Document Storage
    public S3Config getS3DocumentStorage(Long orgId) {
        OrgConfig orgConfig = orgConfigRepository.findByOrgId(orgId)
                .orElseThrow(() -> new RuntimeException("OrgConfig not found for orgId: " + orgId));

        JsonNode integrations = orgConfig.getIntegrations();
        if (integrations == null || !integrations.has("document_storage") || !integrations.get("document_storage").has("s3")) {
            throw new RuntimeException("No S3 document storage config found for orgId=" + orgId);
        }

        JsonNode s3Node = integrations.get("document_storage").get("s3");
        try {
            return objectMapper.treeToValue(s3Node, S3Config.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map S3 config for orgId=" + orgId, e);
        }
    }

    @Data
    public static class S3Config {
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;
    }
}

package com.qiaben.ciyex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public <T> T get(Long orgId, IntegrationKey integrationKey) {
        // Set tenant context before accessing org_config
        RequestContext context = new RequestContext();
        context.setOrgId(orgId);
        RequestContext.set(context);
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
            RequestContext.clear();
        }
    }

    public <T> T getForCurrentOrg(IntegrationKey integrationKey) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return get(orgId, integrationKey);
    }

    @Transactional
    public String getStorageType(Long orgId) {
        // Set tenant context before accessing org_config
        RequestContext context = new RequestContext();
        context.setOrgId(orgId);
        RequestContext.set(context);
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
            RequestContext.clear();
        }
    }

    public String getStorageTypeForCurrentOrg() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        return getStorageType(orgId);
    }

}

/*

@Autowired
private OrgIntegrationConfigProvider integrationConfigProvider;

public void someServiceMethod(Long orgId) {
    OpenEmrConfig openEmrConfig = integrationConfigProvider.get(orgId, IntegrationKey.OPENEMR);
    StripeConfig stripeConfig = integrationConfigProvider.get(orgId, IntegrationKey.STRIPE);
    TwilioConfig twilioConfig = integrationConfigProvider.get(orgId, IntegrationKey.TWILIO);
    // ...and so on

    String storageType = integrationConfigProvider.getStorageType(orgId);
    // Use storageType to resolve the appropriate ExternalOrgStorage
}*/
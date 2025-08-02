package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ExternalOrgStorageResolver {

    private final OrgIntegrationConfigProvider configProvider;
    private final Map<String, ExternalOrgStorage> storageImplementations;

    @Autowired
    public ExternalOrgStorageResolver(
            OrgIntegrationConfigProvider configProvider,
            Map<String, ExternalOrgStorage> storageImplementations) {
        this.configProvider = configProvider;
        this.storageImplementations = storageImplementations;
    }

    public ExternalOrgStorage resolve() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.error("No orgId found in RequestContext");
            throw new IllegalStateException("No orgId in request context");
        }

        String storageType = configProvider.getStorageType(orgId);
        if (storageType == null) {
            log.warn("No storage_type configured for orgId: {}. Using no-op storage.", orgId);
            return storageImplementations.getOrDefault("noOp", new NoOpExternalOrgStorage());
        }

        ExternalOrgStorage storage = storageImplementations.get(storageType);
        if (storage == null) {
            log.error("No ExternalOrgStorage implementation found for storage_type: {} for orgId: {}", storageType, orgId);
            throw new RuntimeException("No ExternalOrgStorage implementation found for storage_type: " + storageType);
        }

        log.debug("Resolved ExternalOrgStorage for orgId: {} to type: {}", orgId, storageType);
        return storage;
    }
}

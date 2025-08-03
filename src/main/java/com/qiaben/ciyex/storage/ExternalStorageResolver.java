package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExternalStorageResolver {

    @Autowired
    private Map<String, List<ExternalStorage<?>>> storageImplementations;

    @Autowired
    private OrgIntegrationConfigProvider configProvider;

    @SuppressWarnings("unchecked")
    public <T> ExternalStorage<T> resolve(Class<T> entityType) {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new IllegalStateException("No storage type configured for current org");
        }

        List<ExternalStorage<?>> storages = storageImplementations.get(storageType);
        if (storages == null || storages.isEmpty()) {
            throw new IllegalStateException("No ExternalStorage implementations found for storage_type: " + storageType);
        }

        ExternalStorage<?> matchingStorage = storages.stream()
                .filter(storage -> storage.supports(entityType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ExternalStorage implementation found for storage_type: " + storageType + " that supports entity type: " + entityType.getName()));

        return (ExternalStorage<T>) matchingStorage;
    }
}
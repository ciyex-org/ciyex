package org.ciyex.ehr.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.stereotype.Component;

/**
 * Resolves the appropriate FileStorageStrategy based on the org's Vaultik app installation config.
 *
 * - If Vaultik is installed and storage_mode = "files-service" → VaultikStorageStrategy
 * - If Vaultik is installed and storage_mode = "local" → LocalFileStorageStrategy
 * - If Vaultik is NOT installed → LocalFileStorageStrategy (fallback)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileStorageStrategyResolver {

    private final AppInstallationService appInstallationService;
    private final VaultikStorageStrategy vaultikStrategy;
    private final LocalFileStorageStrategy localStrategy;

    public FileStorageStrategy resolve(String orgId) {
        try {
            var installation = appInstallationService.getInstallation(orgId, "vaultik");
            if (installation == null) {
                log.debug("Vaultik not installed for org {}, using local storage", orgId);
                return localStrategy;
            }

            var config = installation.getConfig();
            if (config != null) {
                Object mode = config.get("storage_mode");
                if ("local".equals(mode)) {
                    log.debug("Org {} configured for local storage", orgId);
                    return localStrategy;
                }
            }

            return vaultikStrategy;
        } catch (Exception e) {
            log.warn("Error resolving storage strategy for org {}, falling back to local: {}", orgId, e.getMessage());
            return localStrategy;
        }
    }
}

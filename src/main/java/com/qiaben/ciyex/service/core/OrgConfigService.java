package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;

    /**
     * Get config for an org by orgId, with caching.
     */
    @Cacheable(value = "orgConfigs", key = "#orgId")
    public OrgConfig getConfigByOrgId(Long orgId) {
        return orgConfigRepository.findByOrgId(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Config not found for orgId: " + orgId));
    }

    /**
     * Evict org config from cache after update or new addition.
     */
    @CacheEvict(value = "orgConfigs", key = "#orgId")
    public void evictConfigCache(Long orgId) {
        // No implementation needed. Just evicts cache.
    }

    /**
     * Evict org config from cache after deletion.
     */
    @CacheEvict(value = "orgConfigs", key = "#orgId")
    public void evictConfigCacheOnDelete(Long orgId) {
        // No implementation needed. Just evicts cache.
    }
}

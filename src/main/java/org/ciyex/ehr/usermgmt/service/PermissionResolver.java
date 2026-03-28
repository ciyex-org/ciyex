package org.ciyex.ehr.usermgmt.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.usermgmt.entity.RolePermissionConfig;
import org.ciyex.ehr.usermgmt.repository.RolePermissionConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Resolves user permissions from role_permission_config with Caffeine caching.
 * Merges permissions from ALL of a user's Keycloak roles.
 * Falls back to __SYSTEM__ templates if no org-specific role config exists.
 */
@Service
@Slf4j
public class PermissionResolver {

    private final RolePermissionConfigRepository repo;

    private final Cache<String, List<String>> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    private final Cache<String, Integer> priorityCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public PermissionResolver(RolePermissionConfigRepository repo) {
        this.repo = repo;
    }

    /**
     * Resolve merged permissions for a user given their org and all Keycloak roles.
     */
    public List<String> resolve(String orgAlias, Collection<String> roleNames) {
        if (orgAlias == null || roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        Set<String> merged = new LinkedHashSet<>();
        for (String role : roleNames) {
            String normalizedRole = role.toUpperCase();
            String cacheKey = orgAlias + "|" + normalizedRole;
            List<String> perms = cache.get(cacheKey, k -> loadPermissions(orgAlias, normalizedRole));
            if (perms != null) {
                merged.addAll(perms);
            }
        }

        return List.copyOf(merged);
    }

    private List<String> loadPermissions(String orgAlias, String roleName) {
        // Try org-specific first
        Optional<RolePermissionConfig> config = repo.findByOrgAliasAndRoleName(orgAlias, roleName);

        // Fall back to __SYSTEM__ template
        if (config.isEmpty()) {
            config = repo.findByOrgAliasAndRoleName("__SYSTEM__", roleName);
        }

        List<String> permissions = config
                .map(RolePermissionConfig::getPermissions)
                .orElse(List.of());

        log.debug("Resolved {} permissions for role '{}' in org '{}'", permissions.size(), roleName, orgAlias);
        return permissions;
    }

    /**
     * Determine the primary (highest priority) role from the user's roles.
     * Uses display_order from role_permission_config (lower = higher priority).
     */
    public String determinePrimaryRole(String orgAlias, Collection<String> roleNames) {
        if (orgAlias == null || roleNames == null || roleNames.isEmpty()) {
            return null;
        }

        String bestRole = null;
        int bestOrder = Integer.MAX_VALUE;

        for (String role : roleNames) {
            String normalizedRole = role.toUpperCase();
            String cacheKey = orgAlias + "|order|" + normalizedRole;
            Integer order = priorityCache.get(cacheKey, k -> loadDisplayOrder(orgAlias, normalizedRole));
            if (order != null && order < bestOrder) {
                bestOrder = order;
                bestRole = normalizedRole;
            }
        }

        return bestRole;
    }

    private Integer loadDisplayOrder(String orgAlias, String roleName) {
        Optional<RolePermissionConfig> config = repo.findByOrgAliasAndRoleName(orgAlias, roleName);
        if (config.isEmpty()) {
            config = repo.findByOrgAliasAndRoleName("__SYSTEM__", roleName);
        }
        return config.map(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 100).orElse(null);
    }

    /**
     * Evict all cached entries for an org (called when roles are updated/deleted).
     */
    public void evictCache(String orgAlias) {
        cache.asMap().keySet().removeIf(k -> k.startsWith(orgAlias + "|"));
        priorityCache.asMap().keySet().removeIf(k -> k.startsWith(orgAlias + "|"));
        log.debug("Evicted permission cache for org '{}'", orgAlias);
    }
}

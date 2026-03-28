package org.ciyex.ehr.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.usermgmt.entity.RolePermissionConfig;
import org.ciyex.ehr.usermgmt.repository.RolePermissionConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Resolves SMART on FHIR scopes for a role within an org context.
 *
 * <p>Looks up the {@code smart_scopes} JSONB column in {@code role_permission_config}.
 * Falls back to {@code __SYSTEM__} templates, then to the static
 * {@link RolePermissionRegistry} if no DB row exists at all.</p>
 *
 * <p>Results are cached for 5 minutes per org|role key.</p>
 */
@Service
@Slf4j
public class SmartScopeResolver {

    private final RolePermissionConfigRepository repo;

    private final Cache<String, Set<String>> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public SmartScopeResolver(RolePermissionConfigRepository repo) {
        this.repo = repo;
    }

    /**
     * Resolve the union of SMART scopes for a set of roles within an org.
     * Used by {@link KeycloakJwtAuthenticationConverter} at authentication time.
     */
    public Set<String> resolveScopes(String orgAlias, Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return Set.of();

        Set<String> merged = new LinkedHashSet<>();
        for (String role : roleNames) {
            String normalized = role.toUpperCase();
            String cacheKey = (orgAlias != null ? orgAlias : "__NONE__") + "|smart|" + normalized;
            Set<String> scopes = cache.get(cacheKey, k -> loadScopes(orgAlias, normalized));
            if (scopes != null) {
                merged.addAll(scopes);
            }
        }
        return Collections.unmodifiableSet(merged);
    }

    private Set<String> loadScopes(String orgAlias, String roleName) {
        // 1. Try org-specific
        if (orgAlias != null) {
            Optional<RolePermissionConfig> config = repo.findByOrgAliasAndRoleName(orgAlias, roleName);
            if (config.isPresent() && config.get().getSmartScopes() != null
                    && !config.get().getSmartScopes().isEmpty()) {
                log.debug("Loaded {} smart scopes for role '{}' in org '{}'",
                        config.get().getSmartScopes().size(), roleName, orgAlias);
                return new LinkedHashSet<>(config.get().getSmartScopes());
            }
        }

        // 2. Fall back to __SYSTEM__ template
        Optional<RolePermissionConfig> systemConfig = repo.findByOrgAliasAndRoleName("__SYSTEM__", roleName);
        if (systemConfig.isPresent() && systemConfig.get().getSmartScopes() != null
                && !systemConfig.get().getSmartScopes().isEmpty()) {
            log.debug("Loaded {} smart scopes for role '{}' from __SYSTEM__ template",
                    systemConfig.get().getSmartScopes().size(), roleName);
            return new LinkedHashSet<>(systemConfig.get().getSmartScopes());
        }

        // 3. Fall back to static RolePermissionRegistry
        Optional<ClinicalRole> clinicalRole = ClinicalRole.fromString(roleName);
        if (clinicalRole.isPresent()) {
            Set<String> staticScopes = RolePermissionRegistry.permissionsFor(clinicalRole.get());
            log.debug("Fell back to static registry for role '{}': {} scopes", roleName, staticScopes.size());
            return staticScopes;
        }

        return Set.of();
    }

    /**
     * Evict all cached entries for an org (called when roles are updated).
     */
    public void evictCache(String orgAlias) {
        cache.asMap().keySet().removeIf(k -> k.startsWith(orgAlias + "|smart|"));
        log.debug("Evicted smart scope cache for org '{}'", orgAlias);
    }
}

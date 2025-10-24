package com.qiaben.ciyex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to handle tenant access control based on Keycloak groups
 * 
 * Group Hierarchy:
 * - Apps (top level) - Access to all tenants
 *   - Ciyex
 *   - Aran
 * - Tenants (organization level) - Access to specific tenants
 *   - Qiaben Health
 *   - MediPlus
 *   - CareWell
 */
@Service
@Slf4j
public class TenantAccessService {

    private static final String APPS_GROUP_PREFIX = "/Apps";
    private static final String TENANTS_GROUP_PREFIX = "/Tenants";

    /**
     * Check if user has access to all tenants (belongs to Apps group)
     */
    public boolean hasAccessToAllTenants(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        
        return groups.stream()
                .anyMatch(group -> group.startsWith(APPS_GROUP_PREFIX));
    }

    /**
     * Get list of accessible tenant names from user's groups
     */
    public List<String> getAccessibleTenants(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }

        // If user belongs to Apps group, they have access to all tenants
        if (hasAccessToAllTenants(groups)) {
            log.debug("User has Apps group access - returning all tenants");
            return List.of("ALL");
        }

        // Extract tenant names from Tenants group
        List<String> tenants = groups.stream()
                .filter(group -> group.startsWith(TENANTS_GROUP_PREFIX))
                .map(group -> group.substring(TENANTS_GROUP_PREFIX.length() + 1)) // Remove "/Tenants/" prefix
                .collect(Collectors.toList());

        log.debug("User has access to {} specific tenant(s): {}", tenants.size(), tenants);
        return tenants;
    }

    /**
     * Check if user requires X-Org-Id header
     * Required only if user has access to multiple specific tenants
     */
    public boolean requiresOrgIdHeader(List<String> groups) {
        // If user has access to all tenants, they need to specify org
        if (hasAccessToAllTenants(groups)) {
            return true;
        }

        // If user has access to multiple specific tenants, they need to specify which one
        List<String> tenants = getAccessibleTenants(groups);
        return tenants.size() > 1;
    }

    /**
     * Get the default tenant for user (if they have only one)
     */
    public String getDefaultTenant(List<String> groups) {
        if (hasAccessToAllTenants(groups)) {
            return null; // No default for users with full access
        }

        List<String> tenants = getAccessibleTenants(groups);
        if (tenants.size() == 1) {
            return tenants.get(0);
        }

        return null; // Multiple tenants, no default
    }

    /**
     * Validate if user has access to specified tenant
     */
    public boolean hasAccessToTenant(List<String> groups, String tenantName) {
        if (tenantName == null || tenantName.isBlank()) {
            return false;
        }

        // Users with Apps access can access any tenant
        if (hasAccessToAllTenants(groups)) {
            return true;
        }

        // Check if tenant is in user's accessible tenants
        List<String> accessibleTenants = getAccessibleTenants(groups);
        return accessibleTenants.stream()
                .anyMatch(tenant -> tenant.equalsIgnoreCase(tenantName));
    }

    /**
     * Get org ID from group attributes
     * 
     * @param groupAttributes Map of group paths to their attributes from Keycloak
     * @param tenantGroupPath Full group path (e.g., "/Tenants/Qiaben Health")
     * @return Org ID from group attributes, or null if not found
     */
    public Long getOrgIdFromGroupAttributes(Map<String, Map<String, Object>> groupAttributes, String tenantGroupPath) {
        if (groupAttributes == null || !groupAttributes.containsKey(tenantGroupPath)) {
            return null;
        }
        
        Map<String, Object> attrs = groupAttributes.get(tenantGroupPath);
        if (attrs.containsKey("org_id")) {
            try {
                Object orgIdValue = attrs.get("org_id");
                if (orgIdValue instanceof Number) {
                    return ((Number) orgIdValue).longValue();
                } else {
                    return Long.parseLong(orgIdValue.toString());
                }
            } catch (NumberFormatException e) {
                log.error("Invalid org_id in group attributes for {}: {}", tenantGroupPath, attrs.get("org_id"));
            }
        }
        
        return null;
    }

    /**
     * Resolve the org ID for the current request
     * 
     * @param groups User's Keycloak groups
     * @param groupAttributes Group attributes from Keycloak token
     * @param requestedOrgId Org ID from X-Org-Id header (can be null)
     * @return Resolved org ID, or null if cannot be determined
     */
    public Long resolveOrgId(List<String> groups, Map<String, Map<String, Object>> groupAttributes, Long requestedOrgId) {
        // If user has access to all tenants, use the requested org ID
        if (hasAccessToAllTenants(groups)) {
            if (requestedOrgId == null) {
                log.warn("User has full access but no X-Org-Id header provided");
                return null;
            }
            return requestedOrgId;
        }

        // Get tenant groups (filter groups that start with /Tenants/)
        List<String> tenantGroups = groups.stream()
                .filter(group -> group.startsWith(TENANTS_GROUP_PREFIX))
                .collect(Collectors.toList());

        if (tenantGroups.isEmpty()) {
            log.warn("User has no tenant access");
            return null;
        }

        // If user has only one tenant, extract org ID from group attributes
        if (tenantGroups.size() == 1) {
            String tenantGroupPath = tenantGroups.get(0);
            Long orgId = getOrgIdFromGroupAttributes(groupAttributes, tenantGroupPath);
            log.debug("User has single tenant access: {} (orgId: {})", tenantGroupPath, orgId);
            return orgId;
        }

        // User has multiple tenants, must provide X-Org-Id
        if (requestedOrgId == null) {
            log.warn("User has multiple tenant access but no X-Org-Id header provided");
            return null;
        }

        // Validate that requested org ID matches one of user's tenant groups
        for (String tenantGroupPath : tenantGroups) {
            Long groupOrgId = getOrgIdFromGroupAttributes(groupAttributes, tenantGroupPath);
            if (groupOrgId != null && groupOrgId.equals(requestedOrgId)) {
                log.debug("Validated org ID {} for tenant group {}", requestedOrgId, tenantGroupPath);
                return requestedOrgId;
            }
        }

        log.warn("Requested org ID {} not found in user's accessible tenants", requestedOrgId);
        return null;
    }
}

package org.ciyex.ehr.dto.integration;

import lombok.Data;

import java.util.List;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();
    private String authToken;
    private String orgName;  // Organization/Practice name (e.g., "practice_1", "hinisoft")
    private List<String> permissions = List.of();  // Resolved from role_permission_config
    private String userRole;  // Primary Keycloak role (e.g., "ADMIN", "PROVIDER")

    /**
     * Check if user has a specific permission key (exact match).
     */
    public boolean hasPermission(String key) {
        return permissions.contains(key);
    }

    /**
     * Check if user has ANY permission in a category (e.g., "chart" matches "chart.read", "chart.write").
     */
    public boolean hasAnyPermissionInCategory(String category) {
        return permissions.stream().anyMatch(p -> p.startsWith(category + "."));
    }

    /**
     * Check if user has any write-level permission in a category (excludes .read).
     */
    public boolean hasWritePermissionInCategory(String category) {
        return permissions.stream()
                .anyMatch(p -> p.startsWith(category + ".") && !p.endsWith(".read"));
    }

    public static void set(RequestContext ctx) {
        context.set(ctx);
    }

    public static RequestContext get() {
        RequestContext ctx = context.get();
        if (ctx == null) {
            // Create a default, empty RequestContext so callers don't NPE when
            // the interceptor hasn't been registered or a header is missing.
            ctx = new RequestContext();
            context.set(ctx);
        }
        return ctx;
    }

    public static void clear() {
        context.remove();
    }

    // Backward compatibility - deprecated, use getOrgName()
    @Deprecated
    public String getTenantName() {
        return orgName;
    }

    @Deprecated
    public void setTenantName(String tenantName) {
        this.orgName = tenantName;
    }
}
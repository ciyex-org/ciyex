package com.qiaben.ciyex.dto.integration;

import lombok.Data;

import java.util.Locale;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;

    private String tenantName;  // Tenant name (e.g., "practice_1", "hinisoft")
    
    private String schemaName;  // Database schema name from Keycloak group attribute

    /**
     * Temporary compatibility helper for legacy code paths that still expect orgId semantics.
     * Translates numeric orgIds into tenant names and vice versa.
     */
    @Deprecated(forRemoval = true)
    public void setOrgId(Long orgId) {
        if (orgId == null) {
            this.tenantName = null;
            return;
        }
        this.tenantName = "practice_" + orgId;
    }

    /**
     * Temporary compatibility helper for legacy code paths that still expect orgId semantics.
     * Attempts to extract a numeric orgId from the tenant name when possible.
     */
    @Deprecated(forRemoval = true)
    public Long getOrgId() {
        if (tenantName == null || tenantName.isBlank()) {
            return null;
        }
        String normalized = tenantName.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("practice_")) {
            normalized = normalized.substring("practice_".length());
        }
        normalized = normalized.replaceAll("[^0-9]", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static void set(RequestContext ctx) {
        context.set(ctx);
    }

    public static RequestContext get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}


package com.qiaben.ciyex.dto.integration;

import lombok.Data;

import java.util.Locale;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;

    private String tenantName;  // Tenant name (e.g., "practice_1", "hinisoft")
    
    private String schemaName;  // Database schema name from Keycloak group attribute

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


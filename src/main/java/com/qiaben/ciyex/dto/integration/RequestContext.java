package com.qiaben.ciyex.dto.integration;

import lombok.Data;

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
}
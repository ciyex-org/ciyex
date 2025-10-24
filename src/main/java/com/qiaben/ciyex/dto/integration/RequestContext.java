package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;
    private Long orgId;
    private String tenantName;  // Tenant name (e.g., "practice_1", "hinisoft")
    private Long facilityId;
    private String role;

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


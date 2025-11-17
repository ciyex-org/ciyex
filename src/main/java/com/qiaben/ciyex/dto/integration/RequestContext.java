package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;
    private String tenantName;
    private String schemaName;
    private Long orgId;

    public static void set(RequestContext ctx) {
        context.set(ctx);
    }

    public static RequestContext get() {
        RequestContext ctx = context.get();
        if (ctx == null) {
            ctx = new RequestContext();
            ctx.setOrgId(1L); // Default orgId for development
            context.set(ctx);
        }
        return ctx;
    }

    public static void clear() {
        context.remove();
    }
}
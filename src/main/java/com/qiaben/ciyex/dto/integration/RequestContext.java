package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;
    private String tenantName;  // (Old field - now unused, safe to ignore)
    private String schemaName;  // (Old field - now unused, safe to ignore)

    public static void set(RequestContext ctx) {
        context.set(ctx);
    }

    public static RequestContext get() {
        RequestContext ctx = context.get();
        if (ctx == null) {
            ctx = new RequestContext();
            context.set(ctx);
        }
        return ctx;
    }

    public static void clear() {
        context.remove();
    }
}


package com.qiaben.ciyex.dto.core.integration;

import lombok.Data;

@Data
public class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    private String authToken;
    private Long orgId;
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


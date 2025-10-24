package com.qiaben.ciyex.util;

import com.qiaben.ciyex.dto.integration.RequestContext;

/**
 * Utility helper for retrieving current tenant context safely without reintroducing orgId.
 */
public final class TenantContextUtil {

    private TenantContextUtil() {}

    /**
     * Returns current tenant name or null if not set.
     */
    public static String getTenantName() {
        RequestContext rc = RequestContext.get();
        return rc != null ? rc.getTenantName() : null;
    }
}

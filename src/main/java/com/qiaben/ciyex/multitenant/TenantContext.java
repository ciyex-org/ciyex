package com.qiaben.ciyex.multitenant;

/**
 * Thread-local storage for current tenant context.
 * Automatically manages tenant schema selection based on org ID.
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> currentOrgId = new ThreadLocal<>();
    private static final String MASTER_SCHEMA = "public";
    private static final String TENANT_SCHEMA_PREFIX = "practice_";
    
    /**
     * Set the current organization ID for this thread.
     * This will automatically route database operations to the appropriate tenant schema.
     */
    public static void setCurrentOrgId(Long orgId) {
        currentOrgId.set(orgId);
    }
    
    /**
     * Get the current organization ID for this thread.
     */
    public static Long getCurrentOrgId() {
        return currentOrgId.get();
    }
    
    /**
     * Get the current tenant identifier (schema name) based on org ID.
     * Returns master schema for master operations or tenant schema for tenant operations.
     */
    public static String getCurrentTenantId() {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            return MASTER_SCHEMA; // Default to master schema for authentication operations
        }
        return TENANT_SCHEMA_PREFIX + orgId;
    }
    
    /**
     * Check if current context is for master schema operations.
     */
    public static boolean isMasterContext() {
        return getCurrentOrgId() == null;
    }
    
    /**
     * Check if current context is for tenant schema operations.
     */
    public static boolean isTenantContext() {
        return getCurrentOrgId() != null;
    }
    
    /**
     * Clear the current tenant context for this thread.
     * Should be called at the end of request processing.
     */
    public static void clear() {
        currentOrgId.remove();
    }
    
    /**
     * Execute a block of code in master context (for authentication operations).
     */
    public static <T> T executeInMasterContext(java.util.function.Supplier<T> operation) {
        Long previousOrgId = getCurrentOrgId();
        try {
            clear(); // Set to master context
            return operation.get();
        } finally {
            if (previousOrgId != null) {
                setCurrentOrgId(previousOrgId);
            }
        }
    }
    
    /**
     * Execute a block of code in specific tenant context.
     */
    public static <T> T executeInTenantContext(Long orgId, java.util.function.Supplier<T> operation) {
        Long previousOrgId = getCurrentOrgId();
        try {
            setCurrentOrgId(orgId);
            return operation.get();
        } finally {
            if (previousOrgId != null) {
                setCurrentOrgId(previousOrgId);
            } else {
                clear();
            }
        }
    }
}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.multitenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base service that provides automatic tenant context management.
 * Handles schema switching based on org ID without requiring manual tenant selection.
 */
@Service
@Slf4j
public class TenantAwareService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Execute a database operation in the appropriate tenant context.
     * Automatically switches to tenant schema if orgId is provided, otherwise uses master schema.
     */
    @Transactional
    public <T> T executeInTenantContext(Long orgId, Supplier<T> operation) {
        if (orgId == null) {
            // Execute in master context
            return executeInMasterContext(operation);
        }
        
        String originalSchema = getCurrentSchema();
        String tenantSchema = "practice_" + orgId;
        
        try {
            log.debug("Switching to tenant schema: {} for orgId: {}", tenantSchema, orgId);
            setSchema(tenantSchema);
            TenantContext.setCurrentOrgId(orgId);
            return operation.get();
        } finally {
            TenantContext.clear();
            setSchema(originalSchema);
            log.debug("Restored schema to: {}", originalSchema);
        }
    }
    
    /**
     * Execute a database operation in master schema context.
     */
    @Transactional
    public <T> T executeInMasterContext(Supplier<T> operation) {
        String originalSchema = getCurrentSchema();
        
        try {
            log.debug("Executing in master schema context");
            setSchema("public");
            TenantContext.clear(); // Ensure no tenant context
            return operation.get();
        } finally {
            setSchema(originalSchema);
        }
    }
    
    /**
     * Execute a JPA query in the appropriate tenant context.
     */
    @Transactional
    public <T> T executeQueryInTenantContext(Long orgId, Function<EntityManager, T> queryFunction) {
        return executeInTenantContext(orgId, () -> queryFunction.apply(entityManager));
    }
    
    /**
     * Execute a JPA query in master schema context.
     */
    @Transactional
    public <T> T executeQueryInMasterContext(Function<EntityManager, T> queryFunction) {
        return executeInMasterContext(() -> queryFunction.apply(entityManager));
    }
    
    /**
     * Get the current database schema.
     */
    private String getCurrentSchema() {
        try {
            return entityManager.createNativeQuery("SELECT current_schema()")
                    .getSingleResult().toString();
        } catch (Exception e) {
            log.debug("Failed to get current schema, defaulting to public: {}", e.getMessage());
            return "public";
        }
    }
    
    /**
     * Set the database schema for the current connection.
     */
    private void setSchema(String schemaName) {
        try {
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName))
                    .executeUpdate();
            entityManager.flush(); // Ensure the schema change is applied
        } catch (Exception e) {
            log.warn("Failed to set schema to {}: {}", schemaName, e.getMessage());
        }
    }
    
    /**
     * Check if a tenant schema exists.
     */
    @Transactional(readOnly = true)
    public boolean tenantSchemaExists(Long orgId) {
        String schemaName = "practice_" + orgId;
        try {
            Long count = (Long) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = :schemaName")
                    .setParameter("schemaName", schemaName)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            log.warn("Failed to check if schema exists: {}", e.getMessage());
            return false;
        }
    }
}

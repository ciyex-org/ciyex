package com.qiaben.ciyex.aspect;

import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect that automatically switches database schema based on org ID from RequestContext.
 * Works transparently with existing service methods without requiring code changes.
 */
@Aspect
@Component
@Slf4j
public class AutoSchemaAspect {
    
    @PersistenceContext
    private EntityManager entityManager;
    private final DataSource dataSource;

    public AutoSchemaAspect(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && " +
        "(execution(* com.qiaben.ciyex.service..*(..)) || execution(* com.qiaben.ciyex.audit..*(..))) && " +
        "!execution(* com.qiaben.ciyex.service.*SchemaInitializer.*(..)) && " +
        "!execution(* com.qiaben.ciyex.service.TenantAwareService.*(..))")
    public Object autoSwitchSchema(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get org ID from RequestContext
        Long orgId = getOrgIdFromContext();
        
        if (orgId != null) {
            return executeWithTenantSchema(orgId, joinPoint);
        } else {
            return executeWithMasterSchema(joinPoint);
        }
    }
    
    private Object executeWithTenantSchema(Long orgId, ProceedingJoinPoint joinPoint) throws Throwable {
        String originalSchema = getCurrentSchema();
        String tenantSchema = "practice_" + orgId;
        
        try {
            log.debug("Auto-switching to tenant schema: {} for orgId: {} in method: {}", 
                     tenantSchema, orgId, joinPoint.getSignature().getName());
            setSchema(tenantSchema);
            return joinPoint.proceed();
        } finally {
            setSchema(originalSchema);
            log.debug("Restored schema to: {}", originalSchema);
        }
    }
    
    private Object executeWithMasterSchema(ProceedingJoinPoint joinPoint) throws Throwable {
        String originalSchema = getCurrentSchema();
        
        try {
            log.debug("Auto-switching to master schema for method: {}", joinPoint.getSignature().getName());
            setSchema("public");
            return joinPoint.proceed();
        } finally {
            setSchema(originalSchema);
        }
    }
    
    private Long getOrgIdFromContext() {
        try {
            return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        } catch (Exception e) {
            log.debug("Failed to get org ID from RequestContext: {}", e.getMessage());
            return null;
        }
    }
    
    private String getCurrentSchema() {
        try {
            return entityManager.createNativeQuery("SELECT current_schema()")
                    .getSingleResult().toString();
        } catch (Exception e) {
            log.debug("Failed to get current schema, defaulting to public: {}", e.getMessage());
            return "public";
        }
    }
    
    private void setSchema(String schemaName) {
        try {
            // Ensure the tenant schema exists when switching (no-op for public)
            if (!"public".equals(schemaName)) {
                // Execute CREATE SCHEMA on a separate, autocommit connection so it does not participate
                // in the caller's transaction (avoids failures when caller's transaction is read-only)
                try (Connection conn = dataSource.getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        // Ensure this runs immediately; use standard SQL which is supported by Postgres
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                    }
                } catch (Exception ignore) {
                    // best-effort: ignore failures creating schema here
                    log.debug("CREATE SCHEMA for {} failed on separate connection: {}", schemaName, ignore.getMessage());
                }

                // Set search_path to tenant schema first, then public as fallback using a
                // separate autocommit connection so it does not participate in the caller's transaction.
                try (Connection conn2 = dataSource.getConnection(); Statement stmt2 = conn2.createStatement()) {
                    stmt2.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                } catch (Exception ex) {
                    log.warn("Failed to set search_path via separate connection for {}: {}", schemaName, ex.getMessage());
                    // Fall back to EntityManager if necessary
                    entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public").executeUpdate();
                }
            } else {
                try (Connection conn2 = dataSource.getConnection(); Statement stmt2 = conn2.createStatement()) {
                    stmt2.execute("SET search_path TO public");
                } catch (Exception ex) {
                    log.warn("Failed to set search_path to public via separate connection: {}", ex.getMessage());
                    entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
                }
            }

            entityManager.flush(); // Ensure the schema change is applied
        } catch (Exception e) {
            log.warn("Failed to set schema to {}: {}", schemaName, e.getMessage());
        }
    }
}

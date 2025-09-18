package com.qiaben.ciyex.aspect;

import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && " +
            "execution(* com.qiaben.ciyex.service..*(..)) && " +
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
            entityManager.createNativeQuery("SET search_path TO " + schemaName)
                    .executeUpdate();
            entityManager.flush(); // Ensure the schema change is applied
        } catch (Exception e) {
            log.warn("Failed to set schema to {}: {}", schemaName, e.getMessage());
        }
    }
}

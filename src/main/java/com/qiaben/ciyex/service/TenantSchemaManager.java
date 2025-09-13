package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.function.Supplier;

@Slf4j
@Service
public class TenantSchemaManager {
    
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public void executeWithTenantSchema(Runnable operation) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Set schema using native SQL through EntityManager
                entityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS " + schemaName).executeUpdate();
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.debug("Set search_path to: {}, public via EntityManager", schemaName);
                
                // Execute the operation
                operation.run();
                
            } catch (Exception e) {
                log.error("Failed to set schema: {}", schemaName, e);
                throw new RuntimeException("Failed to set tenant schema", e);
            }
        } else {
            // No tenant context, execute normally
            operation.run();
        }
    }
    
    public <T> T executeWithTenantSchema(Supplier<T> operation) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Set schema using native SQL through EntityManager
                entityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS " + schemaName).executeUpdate();
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.debug("Set search_path to: {}, public via EntityManager", schemaName);
                
                // Execute the operation and return result
                return operation.get();
                
            } catch (Exception e) {
                log.error("Failed to set schema: {}", schemaName, e);
                throw new RuntimeException("Failed to set tenant schema", e);
            }
        } else {
            // No tenant context, execute normally
            return operation.get();
        }
    }
    
    public String getCurrentTenantSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            return "practice_" + context.getOrgId();
        }
        return "public";
    }
}

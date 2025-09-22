package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.function.Supplier;

@Slf4j
@Service
public class TenantSchemaManager {
    
    
    @PersistenceContext
    private EntityManager entityManager;
    private final DataSource dataSource;

    public TenantSchemaManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void executeWithTenantSchema(Runnable operation) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Create schema using a separate connection so it doesn't participate in caller's txn
                try (Connection conn = dataSource.getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                    }
                } catch (Exception ignore) {
                    log.debug("CREATE SCHEMA for {} failed on separate connection: {}", schemaName, ignore.getMessage());
                }

                // Set search_path via EntityManager (affects current session)
                entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public").executeUpdate();
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
                try (Connection conn = dataSource.getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                    }
                } catch (Exception ignore) {
                    log.debug("CREATE SCHEMA for {} failed on separate connection: {}", schemaName, ignore.getMessage());
                }

                entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public").executeUpdate();
                log.debug("Set search_path to: {}, public via EntityManager", schemaName);

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

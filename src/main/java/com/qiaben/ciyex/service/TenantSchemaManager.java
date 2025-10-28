package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import javax.sql.DataSource;
import org.springframework.transaction.annotation.Transactional;
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
    
    @Transactional
    public void executeWithTenantSchema(Runnable operation) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            log.debug("TenantSchemaManager.executeWithTenantSchema - RequestContext.orgId={} authTokenPresent={}",
                    (context != null ? context.getOrgId() : null),
                    (context != null && context.getAuthToken() != null));
            
            try {
                // Create schema using a separate connection so it doesn't participate in caller's txn
                try (Connection conn = dataSource.getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                    }
                } catch (Exception ignore) {
                    log.debug("CREATE SCHEMA for {} failed on separate connection: {}", schemaName, ignore.getMessage());
                }
                // Set search_path on the actual JDBC Connection backing the current EntityManager
                Session session = entityManager.unwrap(Session.class);
                try {
                    session.doWork(conn -> {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                        }
                    });
                    log.debug("Set search_path to: {}, public via JDBC connection", schemaName);

                    // Execute the operation while the search_path is set on this connection
                    operation.run();
                } finally {
                    // Reset search_path to public on the same connection to avoid leaking tenant state
                    try {
                        session.doWork(conn -> {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute("SET search_path TO public");
                            }
                        });
                        log.debug("Reset search_path to public after operation for: {}", schemaName);
                    } catch (Exception ignore) {
                        log.debug("Failed to reset search_path after operation for {}: {}", schemaName, ignore.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Failed to set schema: {}", schemaName, e);
                throw new RuntimeException("Failed to set tenant schema", e);
            }
        } else {
            // No tenant context, execute normally
            operation.run();
        }
    }
    
    @Transactional
    public <T> T executeWithTenantSchema(Supplier<T> operation) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            log.debug("TenantSchemaManager.executeWithTenantSchema (supplier) - RequestContext.orgId={} authTokenPresent={}",
                    (context != null ? context.getOrgId() : null),
                    (context != null && context.getAuthToken() != null));
            
            try {
                try (Connection conn = dataSource.getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                    }
                } catch (Exception ignore) {
                    log.debug("CREATE SCHEMA for {} failed on separate connection: {}", schemaName, ignore.getMessage());
                }
                Session session = entityManager.unwrap(Session.class);
                try {
                    session.doWork(conn -> {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                        }
                    });
                    log.debug("Set search_path to: {}, public via JDBC connection", schemaName);

                    return operation.get();
                } finally {
                    try {
                        session.doWork(conn -> {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute("SET search_path TO public");
                            }
                        });
                        log.debug("Reset search_path to public after operation for: {}", schemaName);
                    } catch (Exception ignore) {
                        log.debug("Failed to reset search_path after operation for {}: {}", schemaName, ignore.getMessage());
                    }
                }

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

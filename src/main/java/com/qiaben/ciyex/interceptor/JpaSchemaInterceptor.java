package com.qiaben.ciyex.interceptor;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class JpaSchemaInterceptor implements Interceptor {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setTenantSchema();
        return false;
    }
    
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setTenantSchema();
        return false;
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        setTenantSchema();
        return false;
    }
    
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setTenantSchema();
    }
    
    private void setTenantSchema() {
        RequestContext context = RequestContext.get();
        if (context != null) {
            // Prefer schemaName from Keycloak group attribute, fallback to generated from tenantName
            String schemaName = context.getSchemaName();
            if (schemaName == null && context.getTenantName() != null) {
                schemaName = sanitize(context.getTenantName());
            }
            
            if (schemaName != null) {
                try (Connection connection = dataSource.getConnection();
                     Statement statement = connection.createStatement()) {
                    
                    // Create schema if it doesn't exist
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                    
                    // Set search path to use the tenant schema first
                    statement.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                    
                    log.debug("JPA Interceptor: Set search_path to: {}, public (from Keycloak: {})", 
                             schemaName, context.getSchemaName() != null);
                    
                } catch (SQLException e) {
                    log.error("JPA Interceptor: Failed to set schema: {}", schemaName, e);
                }
            }
        }
    }

    private String sanitize(String tenantName) {
        return tenantName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }
}

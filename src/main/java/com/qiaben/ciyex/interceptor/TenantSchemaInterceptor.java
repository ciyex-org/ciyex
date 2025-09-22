package com.qiaben.ciyex.interceptor;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class TenantSchemaInterceptor implements Interceptor {
    
    private final DataSource dataSource;
    
    public TenantSchemaInterceptor(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setCurrentSchema();
        return false;
    }
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setCurrentSchema();
        return false;
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        setCurrentSchema();
        return false;
    }
    
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        setCurrentSchema();
    }
    
    private void setCurrentSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Create schema if it doesn't exist
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                
                // Set search path to use the tenant schema first, then public
                statement.execute("SET search_path TO " + schemaName + ", public");
                
                log.debug("Set search_path to: {}, public", schemaName);
                
            } catch (SQLException e) {
                log.error("Failed to set schema: {}", schemaName, e);
            }
        }
    }
}

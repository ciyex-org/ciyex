package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Service
public class TenantSchemaService {
    
    @Autowired
    private DataSource dataSource;
    
    public void setTenantSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            setSchema(schemaName);
        } else {
            setSchema("public");
        }
    }
    
    private void setSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create schema if it doesn't exist
            if (!"public".equals(schemaName)) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                log.debug("Ensured schema exists: {}", schemaName);
            }
            
            // Set search path to use the tenant schema first, then public
            String searchPath = "public".equals(schemaName) ? "public" : schemaName + ", public";
            statement.execute("SET search_path TO " + searchPath);
            
            log.debug("Set search_path to: {}", searchPath);
            
        } catch (SQLException e) {
            log.error("Failed to set schema: {}", schemaName, e);
            throw new RuntimeException("Failed to set tenant schema", e);
        }
    }
    
    public String getCurrentSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            return "practice_" + context.getOrgId();
        }
        return "public";
    }
}

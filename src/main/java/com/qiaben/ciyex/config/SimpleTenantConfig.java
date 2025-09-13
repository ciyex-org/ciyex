package com.qiaben.ciyex.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
//@Configuration  // Disabled - using existing TenantIdentifierResolver
public class SimpleTenantConfig {

    public static class TenantSchemaInterceptor {
        private final DataSource dataSource;

        public TenantSchemaInterceptor(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public void setSchema(String schemaName) {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Create schema if it doesn't exist
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                
                // Set search path to use the tenant schema
                statement.execute("SET search_path TO " + schemaName + ", public");
                
                log.debug("Set schema to: {}", schemaName);
                
            } catch (SQLException e) {
                log.error("Failed to set schema: {}", schemaName, e);
            }
        }
    }
}

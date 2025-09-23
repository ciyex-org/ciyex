package com.qiaben.ciyex.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
//@Component  // Disabled - using AOP approach
public class TenantDataSourceProvider {
    
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final ConcurrentHashMap<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    
    public TenantDataSourceProvider(String baseUrl, String username, String password, String driverClassName) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
    }
    
    public DataSource createMasterDataSource() {
        log.info("Creating master datasource for URL: {}", baseUrl);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(baseUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("MasterPool");
        
        return new HikariDataSource(config);
    }
    
    public DataSource createTenantDataSource(String tenantKey) {
        return tenantDataSources.computeIfAbsent(tenantKey, key -> {
            log.info("Creating new tenant datasource for: {}", key);
            
            // Extract database name from base URL
            String databaseName = extractDatabaseName(baseUrl);
            String tenantUrl = baseUrl.replace("/" + databaseName, "/" + databaseName);
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(tenantUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("TenantPool-" + key);
            
            // Set schema for the connection
            config.setSchema(key);
            
            HikariDataSource dataSource = new HikariDataSource(config);
            
            // Ensure schema exists
            ensureSchemaExists(dataSource, key);
            
            return dataSource;
        });
    }
    
    private void ensureSchemaExists(DataSource dataSource, String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create schema if it doesn't exist
            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            statement.execute(createSchemaSQL);
            
            log.info("Schema {} ensured to exist", schemaName);
            
        } catch (SQLException e) {
            log.error("Failed to ensure schema exists for: {}", schemaName, e);
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }
    }
    
    private String extractDatabaseName(String url) {
        // Extract database name from JDBC URL
        // Format: jdbc:postgresql://localhost:5432/databasename
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            String dbPart = url.substring(lastSlash + 1);
            // Remove any query parameters
            int questionMark = dbPart.indexOf('?');
            return questionMark != -1 ? dbPart.substring(0, questionMark) : dbPart;
        }
        return "ciyexdb"; // fallback
    }
    
    public void closeDataSource(String tenantKey) {
        DataSource dataSource = tenantDataSources.remove(tenantKey);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("Closed datasource for tenant: {}", tenantKey);
        }
    }
}

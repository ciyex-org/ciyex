package com.qiaben.ciyex.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
//@Component  // Disabled - using simpler approach
public class MultiTenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> implements HibernatePropertiesCustomizer {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    private final ConcurrentHashMap<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    private DataSource masterDataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        log.debug("Selecting default datasource");
        return getMasterDataSource();
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        log.debug("Selecting datasource for tenant: {}", tenantIdentifier);
        if ("master".equals(tenantIdentifier) || tenantIdentifier == null) {
            return getMasterDataSource();
        }
        return getTenantDataSource(tenantIdentifier);
    }

    private DataSource getMasterDataSource() {
        if (masterDataSource == null) {
            synchronized (this) {
                if (masterDataSource == null) {
                    log.info("Creating master datasource for URL: {}", masterDbUrl);
                    
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(masterDbUrl);
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setDriverClassName(driverClassName);
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(2);
                    config.setConnectionTimeout(30000);
                    config.setIdleTimeout(600000);
                    config.setMaxLifetime(1800000);
                    config.setPoolName("MasterPool");
                    
                    masterDataSource = new HikariDataSource(config);
                }
            }
        }
        return masterDataSource;
    }

    private DataSource getTenantDataSource(String tenantKey) {
        return tenantDataSources.computeIfAbsent(tenantKey, key -> {
            log.info("Creating new tenant datasource for: {}", key);
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(masterDbUrl);
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
            
            return new HikariDataSource(config);
        });
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}

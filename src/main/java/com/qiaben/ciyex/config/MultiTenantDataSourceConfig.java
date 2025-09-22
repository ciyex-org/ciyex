package com.qiaben.ciyex.config;

import com.qiaben.ciyex.multitenant.TenantDataSourceProvider;
import com.qiaben.ciyex.multitenant.TenantRoutingDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

//@Configuration  // Disabled - using simpler approach
public class MultiTenantDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public TenantDataSourceProvider tenantDataSourceProvider() {
        return new TenantDataSourceProvider(masterDbUrl, username, password, driverClassName);
    }

    @Bean
    @Primary
    public DataSource dataSource(TenantDataSourceProvider tenantDataSourceProvider) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();
        
        // Create master datasource for user authentication and org lookup
        DataSource masterDataSource = tenantDataSourceProvider.createMasterDataSource();
        
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTenantDataSourceProvider(tenantDataSourceProvider);
        
        return routingDataSource;
    }
}

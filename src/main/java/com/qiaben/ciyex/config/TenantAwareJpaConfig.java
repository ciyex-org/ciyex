package com.qiaben.ciyex.config;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
//@Configuration  // Disabled - conflicts with existing config
@EnableJpaRepositories(basePackages = "com.qiaben.ciyex.repository")
public class TenantAwareJpaConfig {

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, JpaProperties jpaProperties) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        
        // Wrap the datasource to set schema before each connection
        TenantAwareDataSource tenantDataSource = new TenantAwareDataSource(dataSource);
        em.setDataSource(tenantDataSource);
        em.setPackagesToScan("com.qiaben.ciyex.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        properties.put(AvailableSettings.HBM2DDL_CREATE_NAMESPACES, true);
        
        em.setJpaPropertyMap(properties);
        
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    public static class TenantAwareDataSource implements DataSource {
        private final DataSource delegate;

        public TenantAwareDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = delegate.getConnection();
            setTenantSchema(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = delegate.getConnection(username, password);
            setTenantSchema(connection);
            return connection;
        }

        private void setTenantSchema(Connection connection) {
            RequestContext context = RequestContext.get();
            if (context != null && context.getTenantName() != null) {
                String schemaName = sanitize(context.getTenantName());
                try (Statement statement = connection.createStatement()) {
                    // Create schema if it doesn't exist
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                    
                    // Set search path to use the tenant schema first
                    statement.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                    
                    log.debug("Set search_path to: {}, public for connection", schemaName);
                    
                } catch (SQLException e) {
                    log.error("Failed to set schema: {}", schemaName, e);
                }
            } else {
                log.debug("No tenant context, using default schema");
            }
        }

        private String sanitize(String tenantName) {
            return tenantName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        }

        // Delegate all other methods
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger("TenantAwareDataSource");
        }
    }
}

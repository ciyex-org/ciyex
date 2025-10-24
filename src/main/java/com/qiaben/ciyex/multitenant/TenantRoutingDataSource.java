package com.qiaben.ciyex.multitenant;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    
    private TenantDataSourceProvider tenantDataSourceProvider;
    
    public void setTenantDataSourceProvider(TenantDataSourceProvider provider) {
        this.tenantDataSourceProvider = provider;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        RequestContext context = RequestContext.get();
        
        if (context == null || context.getTenantName() == null) {
            log.debug("No RequestContext or tenantName found, using master datasource");
            return "master";
        }
        String tenantName = context.getTenantName();
        String sanitized = tenantName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        String tenantKey = "practice_" + sanitized;

        log.debug("Routing to tenant datasource (tenantName={}): {}", tenantName, tenantKey);
        return tenantKey;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        
        if ("master".equals(lookupKey)) {
            return super.determineTargetDataSource();
        }
        
        // For tenant-specific datasources, create them dynamically if needed
        DataSource targetDataSource = (DataSource) getResolvedDataSources().get(lookupKey);
        
        if (targetDataSource == null && tenantDataSourceProvider != null) {
            log.info("Creating new tenant datasource for: {}", lookupKey);
            targetDataSource = tenantDataSourceProvider.createTenantDataSource((String) lookupKey);
            
            // Add to resolved datasources for future use
            getResolvedDataSources().put(lookupKey, targetDataSource);
        }
        
        if (targetDataSource == null) {
            log.warn("No datasource found for tenant: {}, falling back to master", lookupKey);
            return getResolvedDefaultDataSource();
        }
        
        return targetDataSource;
    }
}

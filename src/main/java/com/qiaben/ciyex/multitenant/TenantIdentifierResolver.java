package com.qiaben.ciyex.multitenant;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    private static final String DEFAULT_TENANT = "master";

    @Override
    public String resolveCurrentTenantIdentifier() {
        RequestContext context = RequestContext.get();
        
        if (context == null || context.getOrgId() == null) {
            log.debug("No RequestContext or orgId found, using default tenant: {}", DEFAULT_TENANT);
            return DEFAULT_TENANT;
        }
        
        String tenantId = "practice_" + context.getOrgId();
        log.debug("Resolved tenant identifier: {}", tenantId);
        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}

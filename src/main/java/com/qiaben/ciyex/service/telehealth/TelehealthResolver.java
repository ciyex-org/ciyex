package com.qiaben.ciyex.service.telehealth;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.TelehealthConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TelehealthResolver implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final OrgIntegrationConfigProvider configProvider;

    public TelehealthResolver(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public TelehealthService resolve() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        if (tenantName == null || tenantName.isBlank()) {
            // No tenantName available; log and return null so callers can proceed without telehealth.
            // This is intentionally non-fatal to avoid breaking controllers when tenant header is missing.
            return null;
        }

        TelehealthConfig config = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        if (config == null || config.getVendor() == null) {
            // No telehealth config for this tenant; return null to indicate no telehealth available.
            return null;
        }

        String vendor = config.getVendor();
        Map<String, TelehealthService> beans = applicationContext.getBeansOfType(TelehealthService.class);
        return beans.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().isAnnotationPresent(TelehealthVendor.class))
                .filter(entry -> vendor.equalsIgnoreCase(entry.getValue().getClass().getAnnotation(TelehealthVendor.class).value()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalArgumentException("No TelehealthService implementation found for vendor: " + vendor));
    }
}
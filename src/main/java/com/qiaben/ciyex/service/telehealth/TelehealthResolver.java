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
        Long orgId = RequestContext.get().getOrgId();
        if (orgId == null) {
            throw new IllegalStateException("No orgId available in request context");
        }

        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        if (config == null || config.getVendor() == null) {
            throw new IllegalArgumentException("No telehealth config or vendor found for orgId: " + orgId);
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
package org.ciyex.ehr.service.ai;

import org.ciyex.ehr.dto.integration.AiConfig;
import org.ciyex.ehr.dto.integration.IntegrationKey;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.util.OrgIntegrationConfigProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiResolver implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final OrgIntegrationConfigProvider configProvider;

    public AiResolver(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AiService resolve() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalStateException("No tenantName available in request context");
        }

        AiConfig config = configProvider.getForCurrentTenant(IntegrationKey.AI);
        if (config == null || config.getVendor() == null) {
            throw new IllegalArgumentException("No AI config or vendor found for tenantName: " + tenantName);
        }

        String vendor = config.getVendor();
        Map<String, AiService> beans = applicationContext.getBeansOfType(AiService.class);
        return beans.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().isAnnotationPresent(AiVendor.class))
                .filter(entry -> vendor.equalsIgnoreCase(entry.getValue().getClass().getAnnotation(AiVendor.class).value()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalArgumentException("No AiService implementation found for vendor: " + vendor));
    }
}
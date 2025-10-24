package com.qiaben.ciyex.service.ai;

import com.qiaben.ciyex.dto.integration.AiConfig;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AiVendor("mock")
@Component
@Slf4j
public class MockAiService implements AiService {

    private final OrgIntegrationConfigProvider configProvider;

    public MockAiService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public String generateCompletion(String prompt) {
    String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    AiConfig config = configProvider.getForCurrentTenant(IntegrationKey.AI);
        ensureMockConfigured(config);

        String response = config.getMock().getFixedResponse() != null ? config.getMock().getFixedResponse() : "Mock response to: " + prompt;

    log.info("Generated Mock AI completion for tenantName={}, prompt={}", tenantName, prompt);
        return response;
    }

    private static void ensureMockConfigured(AiConfig config) {
        if (config == null || config.getMock() == null) {
            throw new IllegalStateException("Mock AI configuration is missing.");
        }
    }
}
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
        Long orgId = RequestContext.get().getOrgId();
        AiConfig config = configProvider.get(orgId, IntegrationKey.AI);
        ensureMockConfigured(config);

        String response = config.getMock().getFixedResponse() != null ? config.getMock().getFixedResponse() : "Mock response to: " + prompt;

        log.info("Generated Mock AI completion for orgId={}, prompt={}", orgId, prompt);
        return response;
    }

    private static void ensureMockConfigured(AiConfig config) {
        if (config == null || config.getMock() == null) {
            throw new IllegalStateException("Mock AI configuration is missing.");
        }
    }
}
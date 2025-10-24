package com.qiaben.ciyex.service.ai;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.qiaben.ciyex.dto.integration.AiConfig;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@AiVendor("azure")
@Component
@Slf4j
public class AzureAiService implements AiService {

    private final OrgIntegrationConfigProvider configProvider;

    public AzureAiService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public String generateCompletion(String prompt) {
    String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    AiConfig config = configProvider.getForCurrentTenant(IntegrationKey.AI);
        ensureAzureConfigured(config);

        AiConfig.Azure azure = config.getAzure();
        AiConfig.Defaults defaults = config.getDefaults();

        OpenAIClient client = new OpenAIClientBuilder()
                .endpoint(azure.getEndpoint())
                .credential(new AzureKeyCredential(azure.getApiKey()))
                .buildClient();

        List<ChatRequestMessage> messages = Collections.singletonList(new ChatRequestUserMessage(prompt));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                .setTemperature(defaults.getTemperature())
                .setMaxTokens(defaults.getMaxTokens())
                .setTopP(defaults.getTopP());

        ChatResponseMessage response = client.getChatCompletions(azure.getDeployment(), options)
                .getChoices().get(0).getMessage();

    log.info("Generated Azure AI completion for tenantName={}, prompt={}", tenantName, prompt);
        return response.getContent();
    }

    private static void ensureAzureConfigured(AiConfig config) {
        if (config == null || config.getAzure() == null) {
            throw new IllegalStateException("Azure AI configuration is missing.");
        }
    }
}
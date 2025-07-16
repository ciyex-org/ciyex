package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendNumberPoolDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SendNumberPoolService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object sendNumberPoolMessage(SendNumberPoolDto dto) {
        String url = String.format("%s/v2/messages/number_pool", telnyxProperties.getApiBaseUrl());

        // Construct request body map
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_profile_id", dto.getMessagingProfileId());
        body.put("to", dto.getTo());
        body.put("text", dto.getText());
        body.put("subject", dto.getSubject());
        body.put("media_urls", dto.getMediaUrls());
        body.put("webhook_url", dto.getWebhookUrl());
        body.put("webhook_failover_url", dto.getWebhookFailoverUrl());
        body.put("use_profile_webhooks", dto.getUseProfileWebhooks());
        body.put("type", dto.getType());
        body.put("auto_detect", dto.getAutoDetect());

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Object.class);
    }
}

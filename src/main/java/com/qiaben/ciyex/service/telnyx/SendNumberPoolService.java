package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendNumberPoolDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SendNumberPoolService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public SendNumberPoolService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public Object sendNumberPoolMessage(SendNumberPoolDto dto) {
        String url = String.format("%s/v2/messages/number_pool", telnyxProperties.getApiBaseUrl());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(telnyxProperties.getApiKey());

        // Create a request map
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

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }
}

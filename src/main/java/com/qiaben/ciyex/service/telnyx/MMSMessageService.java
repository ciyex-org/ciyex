package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.MMSMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MMSMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public MMSMessageService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public Object sendGroupMMS(MMSMessageDto mmsMessageDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messages/group_mms";

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(telnyxProperties.getApiKey());

        // Map DTO fields to the correct JSON keys for Telnyx
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("from", mmsMessageDto.getFrom());
        requestBody.put("to", mmsMessageDto.getTo());
        requestBody.put("text", mmsMessageDto.getText());
        requestBody.put("subject", mmsMessageDto.getSubject());
        requestBody.put("media_urls", mmsMessageDto.getMediaUrls());
        requestBody.put("webhook_url", mmsMessageDto.getWebhookUrl());
        requestBody.put("webhook_failover_url", mmsMessageDto.getWebhookFailoverUrl());
        requestBody.put("use_profile_webhooks", mmsMessageDto.getUseProfileWebhooks());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
        return response.getBody();
    }
}

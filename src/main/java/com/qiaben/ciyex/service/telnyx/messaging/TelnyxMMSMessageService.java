package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMMSMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxMMSMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object sendGroupMMS(TelnyxMMSMessageDto mmsMessageDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messages/group_mms";

        // Map DTO fields to Telnyx-compatible request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("from", mmsMessageDto.getFrom());
        requestBody.put("to", mmsMessageDto.getTo());
        requestBody.put("text", mmsMessageDto.getText());
        requestBody.put("subject", mmsMessageDto.getSubject());
        requestBody.put("media_urls", mmsMessageDto.getMediaUrls());
        requestBody.put("webhook_url", mmsMessageDto.getWebhookUrl());
        requestBody.put("webhook_failover_url", mmsMessageDto.getWebhookFailoverUrl());
        requestBody.put("use_profile_webhooks", mmsMessageDto.getUseProfileWebhooks());

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Object.class);
    }
}

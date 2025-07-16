package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SendMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object sendMessage(SendMessageDto sendMessageDto) {
        String url = String.format("%s/v2/messages", telnyxProperties.getApiBaseUrl());

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(sendMessageDto)
                .retrieve()
                .body(Object.class);
    }
}

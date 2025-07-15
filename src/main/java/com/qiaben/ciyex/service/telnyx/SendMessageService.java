package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SendMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public SendMessageService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public Object sendMessage(SendMessageDto sendMessageDto) {
        String url = String.format("%s/v2/messages", telnyxProperties.getApiBaseUrl());
        return restTemplate.postForObject(url, sendMessageDto, Object.class);
    }
}

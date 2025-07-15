package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ScheduleMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScheduleMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public ScheduleMessageService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public Object scheduleMessage(ScheduleMessageDto dto) {
        String url = String.format("%s/v2/messages/schedule", telnyxProperties.getApiBaseUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(telnyxProperties.getApiKey());
        HttpEntity<ScheduleMessageDto> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);
        return response.getBody();
    }
}

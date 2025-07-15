package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CancelScheduleMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CancelScheduleMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public CancelScheduleMessageService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public CancelScheduleMessageDto cancelScheduledMessage(String id) {
        String url = String.format("%s/v2/messages/%s", telnyxProperties.getApiBaseUrl(), id);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.set("Accept", "application/json");

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<CancelScheduleMessageDto> response = restTemplate.exchange(
                url, HttpMethod.DELETE, request, CancelScheduleMessageDto.class);

        return response.getBody();
    }
}

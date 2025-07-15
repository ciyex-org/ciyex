package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RetrieveMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RetrieveMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public RetrieveMessageService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public RetrieveMessageDto getMessageById(String id) {
        String url = String.format("%s/v2/messages/%s", telnyxProperties.getApiBaseUrl(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<RetrieveMessageDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                RetrieveMessageDto.class
        );
        return response.getBody();
    }
}

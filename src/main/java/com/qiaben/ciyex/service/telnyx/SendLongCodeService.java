package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendLongCodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SendLongCodeService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public SendLongCodeService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public Object sendLongCodeMessage(SendLongCodeDto dto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messages/long_code";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(telnyxProperties.getApiKey());

        HttpEntity<SendLongCodeDto> entity = new HttpEntity<>(dto, headers);
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        return response.getBody();
    }
}

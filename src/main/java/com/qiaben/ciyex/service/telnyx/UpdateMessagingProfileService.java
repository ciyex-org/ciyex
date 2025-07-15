package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.UpdateMessagingProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class UpdateMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public UpdateMessagingProfileService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public UpdateMessagingProfileDto updateMessagingProfile(String id, UpdateMessagingProfileDto updateMessagingProfileDto) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);

        HttpEntity<UpdateMessagingProfileDto> requestEntity = new HttpEntity<>(updateMessagingProfileDto);

        ResponseEntity<UpdateMessagingProfileDto> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, UpdateMessagingProfileDto.class);

        return response.getBody();
    }
}

package com.qiaben.ciyex.service.telnyx.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsCreateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxPushCredentialsCreateService {

    private final TelnyxProperties telnyxProperties;
    private final ObjectMapper objectMapper;

    public TelnyxPushCredentialsResponseDto createPushCredential(TelnyxPushCredentialsCreateDto dto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/mobile_push_credentials";

        RestClient client = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        try {
            Map<String, Object> response = client.post()
                    .uri(url)
                    .body(dto)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return objectMapper.convertValue(data, TelnyxPushCredentialsResponseDto.class);

        } catch (RestClientResponseException e) {
            throw new RuntimeException("❌ Failed to create push credential: " + e.getResponseBodyAsString(), e);
        }
    }
}

package com.qiaben.ciyex.service.telnyx.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxPushCredentialsListService {

    private final TelnyxProperties telnyxProperties;
    private final ObjectMapper objectMapper;

    public TelnyxPushCredentialsListResponseDto listPushCredentials(String type, String alias, Integer pageSize, Integer pageNumber) {
        String baseUrl = telnyxProperties.getApiBaseUrl() + "/v2/mobile_push_credentials";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        if (type != null) uriBuilder.queryParam("filter[type]", type);
        if (alias != null) uriBuilder.queryParam("filter[alias]", alias);
        if (pageSize != null) uriBuilder.queryParam("page[size]", pageSize);
        if (pageNumber != null) uriBuilder.queryParam("page[number]", pageNumber);

        URI uri = uriBuilder.build().toUri();

        RestClient client = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        try {
            Map<String, Object> response = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            return objectMapper.convertValue(response, TelnyxPushCredentialsListResponseDto.class);

        } catch (RestClientResponseException e) {
            throw new RuntimeException("❌ Failed to list push credentials: " + e.getResponseBodyAsString(), e);
        }
    }
}


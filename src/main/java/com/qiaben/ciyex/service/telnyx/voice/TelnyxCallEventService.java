package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallEventListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import org.springframework.http.HttpHeaders;

@Service
@RequiredArgsConstructor
public class TelnyxCallEventService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxCallEventListResponseDto listCallEvents(Map<String, String> queryParams) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/call_events");

        queryParams.forEach(uriBuilder::queryParam);

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxCallEventListResponseDto.class);
    }
}

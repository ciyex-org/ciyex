package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CallEventListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import org.springframework.http.HttpHeaders;

@Service
@RequiredArgsConstructor
public class CallEventService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public CallEventListResponseDto listCallEvents(Map<String, String> queryParams) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/call_events");

        queryParams.forEach(uriBuilder::queryParam);

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CallEventListResponseDto.class);
    }
}

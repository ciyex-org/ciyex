package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxEnqueueRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxEnqueueResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelnyxEnqueueService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxEnqueueResponseDTO enqueueCall(String callControlId, TelnyxEnqueueRequestDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String uri = "/v2/calls/" + callControlId + "/actions/enqueue";

        try {
            return client.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(TelnyxEnqueueResponseDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to enqueue call: {}", e.getMessage(), e);
            throw e;
        }
    }
}

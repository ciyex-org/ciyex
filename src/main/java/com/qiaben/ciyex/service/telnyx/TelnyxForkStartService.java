package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxForkStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxForkStartResponseDTO;
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
public class TelnyxForkStartService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxForkStartResponseDTO startForking(String callControlId, TelnyxForkStartRequestDTO body) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String uri = "/v2/calls/" + callControlId + "/actions/fork_start";

        try {
            return client.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(TelnyxForkStartResponseDTO.class);
        } catch (RestClientException ex) {
            log.error("Fork start failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

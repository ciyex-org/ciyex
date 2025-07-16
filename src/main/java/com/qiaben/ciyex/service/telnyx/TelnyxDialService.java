package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxDialRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxDialResponseDTO;
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
public class TelnyxDialService {

    private final TelnyxProperties telnyxProperties;


    public TelnyxDialResponseDTO dial(TelnyxDialRequestDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return client.post()
                    .uri("/v2/calls")
                    .body(request)
                    .retrieve()
                    .body(TelnyxDialResponseDTO.class);
        } catch (RestClientException ex) {
            log.error("Dial request failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

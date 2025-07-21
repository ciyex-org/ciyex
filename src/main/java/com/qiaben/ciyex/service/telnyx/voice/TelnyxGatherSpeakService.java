package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherSpeakRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherSpeakResponseDTO;
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
public class TelnyxGatherSpeakService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxGatherSpeakResponseDTO gatherUsingSpeak(String callControlId, TelnyxGatherSpeakRequestDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String uri = "/v2/calls/" + callControlId + "/actions/gather_using_speak";

        try {
            return client.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(TelnyxGatherSpeakResponseDTO.class);
        } catch (RestClientException ex) {
            log.error("Gather using speak failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

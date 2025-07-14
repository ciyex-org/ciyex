package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxGatherAudioRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxGatherAudioResponseDTO;
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
public class TelnyxGatherAudioService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxGatherAudioResponseDTO gatherUsingAudio(String callControlId, TelnyxGatherAudioRequestDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String uri = "/v2/calls/" + callControlId + "/actions/gather_using_audio";

        try {
            return client.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(TelnyxGatherAudioResponseDTO.class);
        } catch (RestClientException ex) {
            log.error("Failed to gather using audio: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

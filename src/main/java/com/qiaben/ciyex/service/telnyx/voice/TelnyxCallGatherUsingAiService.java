package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherUsingAiRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherUsingAiResponseDTO;
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
public class TelnyxCallGatherUsingAiService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxGatherUsingAiResponseDTO gatherUsingAi(String callControlId,
                                                        TelnyxGatherUsingAiRequestDTO request) {

        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String uri = "/v2/calls/" + callControlId + "/actions/gather_using_ai";

        try {
            return client.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(TelnyxGatherUsingAiResponseDTO.class);
        } catch (RestClientException ex) {
            log.error("Gather-using-AI failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxReferCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxReferCallResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxReferCallService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxReferCallResponseDTO referCall(String callControlId, TelnyxReferCallRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/refer";

        RestClient restClient = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient.post()
                .uri("/v2/calls/{callControlId}/actions/refer", callControlId)
                .body(request)
                .retrieve()
                .body(TelnyxReferCallResponseDTO.class);
    }
}

package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateClientStateRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateClientStateResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateClientStateService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxUpdateClientStateResponseDTO updateClientState(String callControlId, TelnyxUpdateClientStateRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/client_state_update";

        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return restClient.method(HttpMethod.PUT)
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(TelnyxUpdateClientStateResponseDTO.class);
        } catch (RestClientResponseException ex) {
            TelnyxUpdateClientStateResponseDTO errorResponse = new TelnyxUpdateClientStateResponseDTO();
            TelnyxUpdateClientStateResponseDTO.DataWrapper data = new TelnyxUpdateClientStateResponseDTO.DataWrapper();
            data.setResult("Error: " + ex.getResponseBodyAsString());
            errorResponse.setData(data);
            return errorResponse;
        }
    }
}

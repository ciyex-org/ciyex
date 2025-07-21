package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHangupCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHangupCallResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxHangupCallService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxHangupCallResponseDTO hangupCall(String callControlId, TelnyxHangupCallRequestDTO requestDTO) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/hangup";

        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient.post()
                .body(requestDTO)
                .retrieve()
                .body(TelnyxHangupCallResponseDTO.class);
    }
}

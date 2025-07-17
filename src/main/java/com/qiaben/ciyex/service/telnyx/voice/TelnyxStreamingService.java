package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStartStreamingRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStopStreamingRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxStreamingService {

    private final TelnyxProperties telnyxProperties;

    public String startStreaming(String callControlId, TelnyxStartStreamingRequestDTO requestDTO) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        return client.post()
                .uri("/calls/{call_control_id}/actions/stream_start", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDTO)
                .retrieve()
                .body(String.class);
    }

    public String stopStreaming(String callControlId, TelnyxStopStreamingRequestDTO requestDTO) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        return client.post()
                .uri("/calls/{call_control_id}/actions/stream_stop", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDTO)
                .retrieve()
                .body(String.class);
    }
}


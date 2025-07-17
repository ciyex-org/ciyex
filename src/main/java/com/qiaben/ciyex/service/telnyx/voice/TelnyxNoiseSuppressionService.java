package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxNoiseSuppressionStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxNoiseSuppressionStopRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxNoiseSuppressionService {

    private final TelnyxProperties telnyxProperties;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + telnyxProperties.getApiKey())
                .build();
    }

    public String start(String callControlId,
                        TelnyxNoiseSuppressionStartRequestDTO dto) {

        return restClient().post()
                .uri("/calls/{id}/actions/noise_suppress_start", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String stop(String callControlId,
                       TelnyxNoiseSuppressionStopRequestDTO dto) {

        return restClient().post()
                .uri("/calls/{id}/actions/noise_suppress_stop", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }
}

package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxPlaybackStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxPlaybackStartResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxPlaybackStartService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxPlaybackStartResponseDTO playAudio(String callControlId, TelnyxPlaybackStartRequestDTO request) {
        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl())
                .path("/v2/calls/")
                .pathSegment(callControlId)
                .path("/actions/playback_start")
                .toUriString();

        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        return restClient.post()
                .body(request)
                .retrieve()
                .body(TelnyxPlaybackStartResponseDTO.class);
    }
}

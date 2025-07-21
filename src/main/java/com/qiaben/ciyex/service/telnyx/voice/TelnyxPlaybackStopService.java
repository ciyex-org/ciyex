package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPlaybackStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPlaybackStopResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxPlaybackStopService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxPlaybackStopResponseDTO stopPlayback(String callControlId, TelnyxPlaybackStopRequestDTO request) {
        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl())
                .path("/v2/calls/")
                .pathSegment(callControlId)
                .path("/actions/playback_stop")
                .toUriString();

        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        return restClient.post()
                .body(request)
                .retrieve()
                .body(TelnyxPlaybackStopResponseDTO.class);
    }
}

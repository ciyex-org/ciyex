// service/telnyx/TelnyxRoomSessionMuteService.java
package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxActionResultDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomSessionMuteService {

    private final TelnyxProperties props;

    public TelnyxActionResultDto mute(String sessionId, TelnyxMuteRequestDto body) {
        RestClient client = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.post()
                .uri("/v2/room_sessions/{sid}/actions/mute", sessionId)
                .body(body)
                .retrieve()
                .body(TelnyxActionResultDto.class);
    }
}

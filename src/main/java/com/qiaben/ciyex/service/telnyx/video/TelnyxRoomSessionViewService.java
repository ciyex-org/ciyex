package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomSessionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomSessionViewService {

    private final TelnyxProperties props;

    public TelnyxRoomSessionResponseDto getRoomSessionById(String sessionId, boolean includeParticipants) {
        RestClient client = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/room_sessions/" + sessionId)
                        .queryParam("include_participants", includeParticipants)
                        .build())
                .retrieve()
                .body(TelnyxRoomSessionResponseDto.class);
    }
}

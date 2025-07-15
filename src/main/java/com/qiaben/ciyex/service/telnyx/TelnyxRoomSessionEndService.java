package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.EndRoomSessionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomSessionEndService {

    private final TelnyxProperties props;

    public EndRoomSessionResponseDto endSession(String sessionId) {
        RestClient client = RestClient.builder()
                .baseUrl(props.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.post()
                .uri("/v2/room_sessions/{sid}/actions/end", sessionId)
                .retrieve()
                .body(EndRoomSessionResponseDto.class);
    }
}

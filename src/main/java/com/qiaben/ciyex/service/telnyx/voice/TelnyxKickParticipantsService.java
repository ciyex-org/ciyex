package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxKickParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxKickParticipantsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxKickParticipantsService {

    private final TelnyxProperties properties;

    public TelnyxKickParticipantsResponseDto kickParticipants(String roomSessionId, TelnyxKickParticipantsRequestDto requestDto) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient.method(HttpMethod.POST)
                .uri("/v2/room_sessions/{room_session_id}/actions/kick", roomSessionId)
                .body(requestDto)
                .retrieve()
                .body(TelnyxKickParticipantsResponseDto.class);
    }
}

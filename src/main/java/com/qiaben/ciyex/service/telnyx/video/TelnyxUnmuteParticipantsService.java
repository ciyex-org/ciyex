package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxUnmuteParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxUnmuteParticipantsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUnmuteParticipantsService {

    private final TelnyxProperties properties;

    public TelnyxUnmuteParticipantsResponseDto unmuteParticipants(String roomSessionId, TelnyxUnmuteParticipantsRequestDto request) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient.method(HttpMethod.POST)
                .uri("/v2/room_sessions/{room_session_id}/actions/unmute", roomSessionId)
                .body(request)
                .retrieve()
                .body(TelnyxUnmuteParticipantsResponseDto.class);
    }
}

package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomResponseDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxUpdateRoomRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateRoomService {
    private final TelnyxProperties telnyxProperties;

    public TelnyxRoomResponseDto updateRoom(String roomId, TelnyxUpdateRoomRequestDto dto) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        return client.patch()
                .uri("/v2/rooms/{roomId}", roomId)
                .body(dto)
                .retrieve()
                .body(TelnyxRoomResponseDto.class);
    }

}

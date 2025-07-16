package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomResponseDto;
import com.qiaben.ciyex.dto.telnyx.UpdateRoomRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UpdateRoomService {
    private final TelnyxProperties telnyxProperties;

    public RoomResponseDto updateRoom(String roomId, UpdateRoomRequestDto dto) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        return client.patch()
                .uri("/v2/rooms/{roomId}", roomId)
                .body(dto)
                .retrieve()
                .body(RoomResponseDto.class);
    }

}

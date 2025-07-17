package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.GenerateTokenRequest;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.RefreshTokenRequest;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.RoomTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomTokenService {

    private final TelnyxProperties properties;

    public RoomTokenResponse generate(String roomId, GenerateTokenRequest req) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.post()
                .uri("/v2/rooms/{roomId}/actions/generate_join_client_token", roomId)
                .body(req)
                .retrieve()
                .body(RoomTokenResponse.class);
    }

    public RoomTokenResponse refresh(String roomId, RefreshTokenRequest req) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.post()
                .uri("/v2/rooms/{roomId}/actions/refresh_client_token", roomId)
                .body(req)
                .retrieve()
                .body(RoomTokenResponse.class);
    }
}

package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.GenerateTokenRequest;
import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.RefreshTokenRequest;
import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.RoomTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RoomTokenService {

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

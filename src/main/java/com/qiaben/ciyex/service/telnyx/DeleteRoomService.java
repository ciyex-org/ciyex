package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class DeleteRoomService {

    private final TelnyxProperties telnyxProperties;

    public void deleteRoom(String roomId) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        client.delete()
                .uri("/v2/rooms/{roomId}", roomId)
                .retrieve();
    }
}

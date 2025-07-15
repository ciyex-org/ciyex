package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ViewRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class ViewRoomService {

    private final TelnyxProperties telnyxProperties;

    public ViewRoomDto getRoomDetails(String roomId, boolean includeSessions) {
        String url = telnyxProperties.getApiUrl() + "/rooms/" + roomId;
        if (includeSessions) {
            url += "?include_sessions=true";
        }

        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader("Accept", "application/json")
                .build();

        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder.path("/rooms/{roomId}")
                            .queryParam("include_sessions", includeSessions)
                            .build(roomId))
                    .retrieve()
                    .body(ViewRoomDto.class);
        } catch (HttpClientErrorException e) {
            System.err.println("Telnyx API returned error: " + e.getResponseBodyAsString());
            return null;
        }
    }
}

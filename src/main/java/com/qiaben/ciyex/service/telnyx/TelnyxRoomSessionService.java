package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomSessionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelnyxRoomSessionService {

    private final TelnyxProperties props;

    public RoomSessionListResponseDto listRoomSessions(
            Optional<String> roomId,
            Optional<String> createdGte,
            Optional<String> createdLte,
            Optional<Boolean> active,
            boolean includeParticipants,
            int pageSize,
            int pageNumber) {

        RestClient client = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v2/room_sessions")
                            .queryParam("include_participants", includeParticipants)
                            .queryParam("page[size]", pageSize)
                            .queryParam("page[number]", pageNumber);
                    roomId.ifPresent(val -> uriBuilder.queryParam("filter[room_id]", val));
                    createdGte.ifPresent(val -> uriBuilder.queryParam("filter[date_created_at][gte]", val));
                    createdLte.ifPresent(val -> uriBuilder.queryParam("filter[date_created_at][lte]", val));
                    active.ifPresent(val -> uriBuilder.queryParam("filter[active]", val));
                    return uriBuilder.build();
                })
                .retrieve()
                .body(RoomSessionListResponseDto.class);
    }
}

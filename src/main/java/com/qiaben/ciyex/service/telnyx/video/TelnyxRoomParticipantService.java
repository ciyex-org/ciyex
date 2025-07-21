package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomParticipantListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelnyxRoomParticipantService {

    private final TelnyxProperties props;

    public TelnyxRoomParticipantListDto listParticipants(
            String roomSessionId,
            Optional<String> joinedGte,
            Optional<String> context,
            int pageSize,
            int pageNumber) {

        RestClient client = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.get()
                .uri(uri -> {
                    uri.path("/v2/room_sessions/{id}/participants")
                            .queryParam("page[size]", pageSize)
                            .queryParam("page[number]", pageNumber);
                    joinedGte.ifPresent(v ->
                            uri.queryParam("filter[date_joined_at][gte]", v));
                    context.ifPresent(v ->
                            uri.queryParam("filter[context]", v));
                    return uri.build(roomSessionId);
                })
                .retrieve()
                .body(TelnyxRoomParticipantListDto.class);
    }
}

package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SessionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
@Service
@RequiredArgsConstructor
public class SessionListResponseService
{
    private final TelnyxProperties telnyxProperties;

    public SessionListResponseDto listSessions(
            String roomId,
            String createdGte,
            Boolean includeParticipants,
            Integer pageSize,
            Integer pageNumber
    )
    {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        return client.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v2/rooms/{roomId}/sessions")
                            .queryParamIfPresent("filter[date_created_at][gte]",
                                    createdGte != null ? java.util.Optional.of(createdGte) : java.util.Optional.empty())
                            .queryParam("include_participants", includeParticipants)
                            .queryParam("page[size]", pageSize)
                            .queryParam("page[number]", pageNumber);
                    return uriBuilder.build(roomId);
                })
                .retrieve()
                .body(SessionListResponseDto.class);
    }
}

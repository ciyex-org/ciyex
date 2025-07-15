package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomParticipantDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomParticipantService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public RoomParticipantDto.RoomParticipantListResponse getRoomParticipants(Map<String, String> queryParams) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(telnyxProperties.getApiBaseUrl() + "/v2/room_participants");
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(RoomParticipantDto.RoomParticipantListResponse.class);
    }

    public RoomParticipantDto.RoomParticipantSingleResponse getRoomParticipantById(String id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/room_participants/" + id;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(RoomParticipantDto.RoomParticipantSingleResponse.class);
    }
}

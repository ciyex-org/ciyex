package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomDto;
import com.qiaben.ciyex.dto.telnyx.RoomRequestDto;
import com.qiaben.ciyex.dto.telnyx.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxRoomService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public RoomDto getRooms(Map<String, String> queryParams) {
        String baseUrl = telnyxProperties.getApiBaseUrl() + "/v2/rooms";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        queryParams.forEach(builder::queryParam);

        return restClient
                .get()
                .uri(builder.toUriString())
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RoomDto.class);
    }

    public RoomResponseDto createRoom(RoomRequestDto roomRequestDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/rooms";

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(roomRequestDto)
                .retrieve()
                .body(RoomResponseDto.class);
    }
}

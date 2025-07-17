package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomCompositionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxRoomCompositionListService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    public TelnyxRoomCompositionListResponseDto listRoomCompositions(Map<String, String> filters) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.getApiBaseUrl() + "/v2/room_compositions");

        filters.forEach(builder::queryParam);

        return restClient.get()
                .uri(builder.toUriString())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxRoomCompositionListResponseDto.class);
    }
}

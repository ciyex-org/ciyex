package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomRecordingListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomRecordingListService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public RoomRecordingListResponseDto listRoomRecordings(Map<String, String> filters) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/room_recordings");

        filters.forEach(builder::queryParam);

        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(RoomRecordingListResponseDto.class);
    }
}

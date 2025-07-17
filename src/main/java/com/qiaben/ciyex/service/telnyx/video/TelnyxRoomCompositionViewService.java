package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomCompositionViewResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomCompositionViewService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxRoomCompositionViewResponseDto getRoomComposition(String compositionId) {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/room_compositions/{id}", compositionId)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(RoomCompositionViewResponseWrapper.class)
                .data(); // ✅ Use `.data()` instead of `.getData()`
    }

    private record RoomCompositionViewResponseWrapper(TelnyxRoomCompositionViewResponseDto data) {}
}

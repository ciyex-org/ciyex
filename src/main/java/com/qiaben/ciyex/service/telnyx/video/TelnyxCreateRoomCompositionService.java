package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxCreateRoomCompositionRequestDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxCreateRoomCompositionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCreateRoomCompositionService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    public TelnyxCreateRoomCompositionResponseDto createComposition(TelnyxCreateRoomCompositionRequestDto request) {
        return restClient.post()
                .uri(properties.getApiBaseUrl() + "/v2/room_compositions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxCreateRoomCompositionResponseDto.class);
    }
}

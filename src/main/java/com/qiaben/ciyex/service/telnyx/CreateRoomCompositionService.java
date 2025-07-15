package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CreateRoomCompositionRequestDto;
import com.qiaben.ciyex.dto.telnyx.CreateRoomCompositionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CreateRoomCompositionService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    public CreateRoomCompositionResponseDto createComposition(CreateRoomCompositionRequestDto request) {
        return restClient.post()
                .uri(properties.getApiBaseUrl() + "/v2/room_compositions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(CreateRoomCompositionResponseDto.class);
    }
}

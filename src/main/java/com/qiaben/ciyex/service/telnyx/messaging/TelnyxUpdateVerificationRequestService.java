package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateVerificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateVerificationRequestService {

    private final TelnyxProperties properties;

    public TelnyxUpdateVerificationResponseDTO updateRequest(String id, TelnyxUpdateVerificationRequestDTO dto) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.patch()
                .uri("/v2/messaging_tollfree/verification/requests/{id}", id)
                .body(dto)
                .retrieve()
                .body(TelnyxUpdateVerificationResponseDTO.class);
    }
}

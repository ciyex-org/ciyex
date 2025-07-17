package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxSubmitVerificationRequestService {

    private final TelnyxProperties properties;

    public TelnyxSubmitVerificationResponseDTO submitRequest(TelnyxSubmitVerificationRequestDTO dto) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.post()
                .uri("/v2/messaging_tollfree/verification/requests")
                .body(dto)
                .retrieve()
                .body(TelnyxSubmitVerificationResponseDTO.class);
    }
}

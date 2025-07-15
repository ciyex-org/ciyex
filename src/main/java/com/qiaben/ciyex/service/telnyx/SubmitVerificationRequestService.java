package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SubmitVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.SubmitVerificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SubmitVerificationRequestService {

    private final TelnyxProperties properties;

    public SubmitVerificationResponseDTO submitRequest(SubmitVerificationRequestDTO dto) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.post()
                .uri("/v2/messaging_tollfree/verification/requests")
                .body(dto)
                .retrieve()
                .body(SubmitVerificationResponseDTO.class);
    }
}

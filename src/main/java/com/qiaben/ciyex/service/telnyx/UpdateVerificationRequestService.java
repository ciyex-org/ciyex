package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.UpdateVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateVerificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UpdateVerificationRequestService {

    private final TelnyxProperties properties;

    public UpdateVerificationResponseDTO updateRequest(String id, UpdateVerificationRequestDTO dto) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.patch()
                .uri("/v2/messaging_tollfree/verification/requests/{id}", id)
                .body(dto)
                .retrieve()
                .body(UpdateVerificationResponseDTO.class);
    }
}

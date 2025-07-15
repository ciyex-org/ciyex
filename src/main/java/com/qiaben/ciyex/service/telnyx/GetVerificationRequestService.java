package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.GetVerificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GetVerificationRequestService {

    private final TelnyxProperties properties;

    public GetVerificationResponseDTO getVerificationRequestById(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        return client.get()
                .uri("/v2/messaging_tollfree/verification/requests/{id}", id)
                .retrieve()
                .body(GetVerificationResponseDTO.class);
    }
}

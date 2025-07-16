package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class DeleteVerificationRequestService {

    private final TelnyxProperties properties;

    public String deleteVerificationRequest(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        ResponseEntity<String> response = client.delete()
                .uri("/v2/messaging_tollfree/verification/requests/{id}", id)
                .retrieve()
                .toEntity(String.class);

        return response.getBody() != null ? response.getBody() : "Deleted successfully";
    }
}

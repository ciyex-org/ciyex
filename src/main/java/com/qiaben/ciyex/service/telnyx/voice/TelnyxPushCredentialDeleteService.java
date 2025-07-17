package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class TelnyxPushCredentialDeleteService {

    private final TelnyxProperties telnyxProperties;

    public void deletePushCredentialById(String pushCredentialId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/mobile_push_credentials/" + pushCredentialId;

        RestClient client = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        try {
            client.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity(); // DELETE with no return body (204)
        } catch (RestClientResponseException e) {
            throw new RuntimeException("❌ Failed to delete push credential: " + e.getResponseBodyAsString(), e);
        }
    }
}

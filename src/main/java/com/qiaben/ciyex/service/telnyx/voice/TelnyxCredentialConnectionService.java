package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCredentialConnectionService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxCredentialConnectionResponseDto getCredentialConnectionById(String id) {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxCredentialConnectionResponseDto.class);
    }

    public TelnyxCredentialConnectionListResponseDto listCredentialConnections() {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxCredentialConnectionListResponseDto.class);
    }

    public TelnyxCredentialConnectionResponseDto updateCredentialConnection(String id, TelnyxCredentialConnectionUpdateDto dto) {
        return restClient.patch()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(dto)
                .retrieve()
                .body(TelnyxCredentialConnectionResponseDto.class);
    }

    public void deleteCredentialConnection(String id) {
        restClient.delete()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .toBodilessEntity();
    }

    public TelnyxCredentialConnectionRegistrationStatusDto checkRegistrationStatus(String id) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}/actions/check_registration_status", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxCredentialConnectionRegistrationStatusDto.class);
    }


    public TelnyxCredentialConnectionResponseDto createCredentialConnection(TelnyxCredentialConnectionCreateDto dto) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(dto)
                .retrieve()
                .body(TelnyxCredentialConnectionResponseDto.class);
    }
}

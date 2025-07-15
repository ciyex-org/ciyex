package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CredentialConnectionService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public CredentialConnectionResponseDto getCredentialConnectionById(String id) {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CredentialConnectionResponseDto.class);
    }

    public CredentialConnectionListResponseDto listCredentialConnections() {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CredentialConnectionListResponseDto.class);
    }

    public CredentialConnectionResponseDto updateCredentialConnection(String id, CredentialConnectionUpdateDto dto) {
        return restClient.patch()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(dto)
                .retrieve()
                .body(CredentialConnectionResponseDto.class);
    }

    public void deleteCredentialConnection(String id) {
        restClient.delete()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .toBodilessEntity();
    }

    public CredentialConnectionRegistrationStatusDto checkRegistrationStatus(String id) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections/{id}/actions/check_registration_status", id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CredentialConnectionRegistrationStatusDto.class);
    }


    public CredentialConnectionResponseDto createCredentialConnection(CredentialConnectionCreateDto dto) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/credential_connections")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(dto)
                .retrieve()
                .body(CredentialConnectionResponseDto.class);
    }
}

// src/main/java/com/qiaben/ciyex/service/telnyx/ExternalConnectionService.java
package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxPhoneNumberListResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxPhoneNumberRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelnyxExternalConnectionService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;


    /**
     * Retrieves external connections page-by-page with optional filters.
     */
    public TelnyxExternalConnectionListResponseDTO listExternalConnections(
            Integer pageNumber,
            Integer pageSize,
            String connectionNameContains,
            String externalSipConnection,
            String id,
            String createdAt,
            String phoneNumberEq
    ) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        String url = UriComponentsBuilder.fromPath("/v2/external_connections")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .queryParam("filter[connection_name][contains]", connectionNameContains)
                .queryParam("filter[external_sip_connection]", externalSipConnection)
                .queryParam("filter[id]", id)
                .queryParam("filter[created_at]", createdAt)
                .queryParam("filter[phone_number][eq]", phoneNumberEq)
                .build()
                .toUriString();

        return client.get()
                .uri(url)
                .retrieve()
                .body(TelnyxExternalConnectionListResponseDTO.class);
    }

    public TelnyxExternalConnectionDTO createExternalConnection(TelnyxExternalConnectionDTO request) {

        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        // Telnyx wraps the response inside {"data":{...}}
        record TelnyxWrapped(TelnyxExternalConnectionDTO data) {}

        TelnyxWrapped wrapped = client.post()
                .uri("/v2/external_connections")
                .body(request)
                .retrieve()
                .body(TelnyxWrapped.class);

        return wrapped != null ? wrapped.data() : null;
    }
    public TelnyxExternalConnectionDTO getExternalConnectionById(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        record TelnyxWrapped(TelnyxExternalConnectionDTO data) {}

        TelnyxWrapped response = client.get()
                .uri("/v2/external_connections/" + id)
                .retrieve()
                .body(TelnyxWrapped.class);

        return response != null ? response.data() : null;
    }

    public TelnyxExternalConnectionDTO updateExternalConnection(String id, TelnyxExternalConnectionDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        record TelnyxWrapped(TelnyxExternalConnectionDTO data) {}

        TelnyxWrapped response = client.patch()
                .uri("/v2/external_connections/" + id)
                .body(request)
                .retrieve()
                .body(TelnyxWrapped.class);

        return response != null ? response.data() : null;
    }

    public TelnyxExternalConnectionDTO deleteExternalConnection(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        record TelnyxWrapped(TelnyxExternalConnectionDTO data) {}

        TelnyxWrapped response = client.delete()
                .uri("/v2/external_connections/" + id)
                .retrieve()
                .body(TelnyxWrapped.class);

        return response != null ? response.data() : null;
    }

    public TelnyxCivicAddressListResponseDTO getCivicAddresses(String externalConnectionId, List<String> countries) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/v2/external_connections/" + externalConnectionId + "/civic_addresses");

        if (countries != null && !countries.isEmpty()) {
            for (String country : countries) {
                uriBuilder.queryParam("filter[country]", country);
            }
        }

        String uri = uriBuilder.build().toUriString();

        return client.get()
                .uri(uri)
                .retrieve()
                .body(TelnyxCivicAddressListResponseDTO.class);
    }

    public TelnyxCivicAddressListResponseDTO.CivicAddress getCivicAddressById(String connectionId, String addressId) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        TelnyxCivicAddressResponseDTO response = client.get()
                .uri("/v2/external_connections/" + connectionId + "/civic_addresses/" + addressId)
                .retrieve()
                .body(TelnyxCivicAddressResponseDTO.class);

        return response != null ? response.getData() : null;
    }
    public TelnyxUploadResponseDTO createUploadRequest(String connectionId, TelnyxUploadRequestDTO request) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        return client.post()
                .uri("/v2/external_connections/{id}/uploads", connectionId)
                .body(request)
                .retrieve()
                .body(TelnyxUploadResponseDTO.class);
    }

    public TelnyxUploadListResponseDTO listUploadRequests(
            String externalConnectionId,
            Integer pageNumber,
            Integer pageSize,
            String statusEq,
            String civicAddressIdEq,
            String locationIdEq,
            String phoneNumberEq,
            String phoneNumberContains
    ) {
        String url = UriComponentsBuilder
                .fromPath("/v2/external_connections/{id}/uploads")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .queryParamIfPresent("filter[status][eq]", Optional.ofNullable(statusEq))
                .queryParamIfPresent("filter[civic_address_id][eq]", Optional.ofNullable(civicAddressIdEq))
                .queryParamIfPresent("filter[location_id][eq]", Optional.ofNullable(locationIdEq))
                .queryParamIfPresent("filter[phone_number][eq]", Optional.ofNullable(phoneNumberEq))
                .queryParamIfPresent("filter[phone_number][contains]", Optional.ofNullable(phoneNumberContains))
                .build(externalConnectionId)
                .toString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TelnyxUploadListResponseDTO.class);
    }

    public boolean refreshUploadStatus(String externalConnectionId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/external_connections/{id}/uploads/refresh";

        Map<String, String> uriVariables = Map.of("id", externalConnectionId);

        Map<String, Object> response = restClient.post()
                .uri(url, uriVariables)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return (Boolean) response.getOrDefault("success", false);
    }

    public TelnyxUploadStatusCountResponseDTO getUploadStatusCount(String externalConnectionId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/external_connections/{id}/uploads/status";

        return restClient.get()
                .uri(url, Map.of("id", externalConnectionId))
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxUploadStatusCountResponseDTO.class);
    }

    public TelnyxUploadRequestDetailResponseDTO getUploadRequestDetail(String connectionId, String ticketId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/external_connections/{id}/uploads/{ticket_id}";

        return restClient.get()
                .uri(url, Map.of("id", connectionId, "ticket_id", ticketId))
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxUploadRequestDetailResponseDTO.class);
    }

    public TelnyxUploadRequestDetailResponseDTO retryUploadRequest(Long externalConnectionId, String ticketId) {
        String url = UriComponentsBuilder.fromHttpUrl(telnyxProperties.getApiBaseUrl())
                .path("/v2/external_connections/{id}/uploads/{ticket_id}/retry")
                .buildAndExpand(externalConnectionId, ticketId)
                .toUriString();

        return restClient.post()
                .uri(url)
                .retrieve()
                .body(TelnyxUploadRequestDetailResponseDTO.class);
    }

    public TelnyxReleaseListResponseDTO listReleases(
            String connectionId,
            Integer pageNumber,
            Integer pageSize,
            String statusEq,
            String civicAddressIdEq,
            String locationIdEq,
            String phoneNumberEq,
            String phoneNumberContains
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(telnyxProperties.getApiBaseUrl() + "/v2/external_connections/{id}/releases")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize);

        if (statusEq != null) uriBuilder.queryParam("filter[status][eq]", statusEq);
        if (civicAddressIdEq != null) uriBuilder.queryParam("filter[civic_address_id][eq]", civicAddressIdEq);
        if (locationIdEq != null) uriBuilder.queryParam("filter[location_id][eq]", locationIdEq);
        if (phoneNumberEq != null) uriBuilder.queryParam("filter[phone_number][eq]", phoneNumberEq);
        if (phoneNumberContains != null) uriBuilder.queryParam("filter[phone_number][contains]", phoneNumberContains);

        return restClient.get()
                .uri(uriBuilder.build(Map.of("id", connectionId)))
                .retrieve()
                .body(TelnyxReleaseListResponseDTO.class);
    }
    public TelnyxReleaseListResponseDTO.Release getReleaseById(String externalConnectionId, String releaseId) {
        String url = String.format("%s/v2/external_connections/%s/releases/%s",
                telnyxProperties.getApiBaseUrl(), externalConnectionId, releaseId);

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxReleaseListResponseDTO.Release.class);
    }

    public TelnyxPhoneNumberListResponseDTO listPhoneNumbers(
            String externalConnectionId,
            Integer pageNumber,
            Integer pageSize,
            String phoneNumberEq,
            String phoneNumberContains,
            String civicAddressId,
            String locationId
    ) {
        StringBuilder uriBuilder = new StringBuilder(String.format(
                "%s/v2/external_connections/%s/phone_numbers?page[number]=%d&page[size]=%d",
                telnyxProperties.getApiBaseUrl(), externalConnectionId, pageNumber, pageSize
        ));

        if (phoneNumberEq != null)
            uriBuilder.append("&filter[phone_number][eq]=").append(phoneNumberEq);
        if (phoneNumberContains != null)
            uriBuilder.append("&filter[phone_number][contains]=").append(phoneNumberContains);
        if (civicAddressId != null)
            uriBuilder.append("&filter[civic_address_id][eq]=").append(civicAddressId);
        if (locationId != null)
            uriBuilder.append("&filter[location_id][eq]=").append(locationId);

        return restClient.get()
                .uri(uriBuilder.toString())
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxPhoneNumberListResponseDTO.class);
    }
    public TelnyxPhoneNumberListResponseDTO.PhoneNumber getPhoneNumberById(String externalConnectionId, String phoneNumberId) {
        String uri = String.format(
                "%s/v2/external_connections/%s/phone_numbers/%s",
                telnyxProperties.getApiBaseUrl(), externalConnectionId, phoneNumberId
        );

        return restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxPhoneNumberListResponseDTO.PhoneNumber.class);
    }

    public TelnyxPhoneNumberListResponseDTO.PhoneNumber updatePhoneNumber(
            String connectionId, String phoneNumberId, TelnyxPhoneNumberRequestDTO request
    ) {
        String url = String.format("%s/v2/external_connections/%s/phone_numbers/%s",
                telnyxProperties.getApiBaseUrl(), connectionId, phoneNumberId);

        return restClient.patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxPhoneNumberListResponseDTO.PhoneNumber.class);
    }

    public TelnyxCivicAddressListResponseDTO.Location updateStaticEmergencyAddress(String externalId, String locationId, String addressId) {
        String url = String.format("%s/v2/external_connections/%s/locations/%s", telnyxProperties.getApiBaseUrl(), externalId, locationId);

        Map<String, String> body = Map.of("static_emergency_address_id", addressId);

        return restClient.patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(body)
                .retrieve()
                .body(TelnyxCivicAddressListResponseDTO.Location.class);
    }

    public TelnyxOperatorConnectRefreshResponseDTO refreshOperatorConnectIntegration() {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/operator_connect/actions/refresh";

        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxOperatorConnectRefreshResponseDTO.class);
    }



}

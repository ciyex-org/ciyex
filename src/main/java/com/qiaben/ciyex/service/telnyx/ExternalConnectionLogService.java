package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ExternalConnectionLogMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ExternalConnectionLogService {

    private final TelnyxProperties telnyxProperties;

    public ExternalConnectionLogMessageDTO getLogMessages(
            Integer pageNumber,
            Integer pageSize,
            String externalConnectionId,
            String telephoneNumberContains,
            String telephoneNumberEq
    ) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        String uri = UriComponentsBuilder.fromPath("/v2/external_connections/log_messages")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .queryParam("filter[external_connection_id]", externalConnectionId)
                .queryParam("filter[telephone_number][contains]", telephoneNumberContains)
                .queryParam("filter[telephone_number][eq]", telephoneNumberEq)
                .build()
                .toUriString();

        return client.get()
                .uri(uri)
                .retrieve()
                .body(ExternalConnectionLogMessageDTO.class);
    }

    public ExternalConnectionLogMessageDTO getLogMessageById(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        String uri = "/v2/external_connections/log_messages/" + id;

        return client.get()
                .uri(uri)
                .retrieve()
                .body(ExternalConnectionLogMessageDTO.class);
    }

    public boolean dismissLogMessageById(String id) {
        RestClient client = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        record TelnyxDeleteResponse(boolean success) {}

        TelnyxDeleteResponse response = client.delete()
                .uri("/v2/external_connections/log_messages/" + id)
                .retrieve()
                .body(TelnyxDeleteResponse.class);

        return response != null && response.success();
    }
}

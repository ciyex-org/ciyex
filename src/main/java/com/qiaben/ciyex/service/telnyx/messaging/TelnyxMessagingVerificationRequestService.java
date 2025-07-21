package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingVerificationRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxMessagingVerificationRequestService {

    private final TelnyxProperties properties;

    public TelnyxMessagingVerificationRequestDTO listVerificationRequests(
            int page,
            int pageSize,
            String dateStart,
            String dateEnd,
            String status,
            String phoneNumber
    ) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        String uri = UriComponentsBuilder.fromPath("/v2/messaging_tollfree/verification/requests")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .queryParamIfPresent("date_start", dateStart == null ? null : java.util.Optional.of(dateStart))
                .queryParamIfPresent("date_end", dateEnd == null ? null : java.util.Optional.of(dateEnd))
                .queryParamIfPresent("status", status == null ? null : java.util.Optional.of(status))
                .queryParamIfPresent("phone_number", phoneNumber == null ? null : java.util.Optional.of(phoneNumber))
                .toUriString();

        return client.get()
                .uri(uri)
                .retrieve()
                .body(TelnyxMessagingVerificationRequestDTO.class);
    }
}

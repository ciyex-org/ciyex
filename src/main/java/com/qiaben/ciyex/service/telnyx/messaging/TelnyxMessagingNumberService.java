
// src/main/java/com/qiaben/ciyex/service/telnyx/MessagingNumberService.java
package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListPhoneNumbersResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingPhoneNumberDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdatePhoneNumberRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxBulkUpdateRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxBulkUpdateResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxMessagingNumberService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .build();
    }

    // List numbers
    public TelnyxListPhoneNumbersResponseDTO list(int pageNumber, int pageSize) {
        String url = UriComponentsBuilder
                .fromPath("/v2/phone_numbers/messaging")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .toUriString();

        return client().get()
                .uri(url)
                .retrieve()
                .body(TelnyxListPhoneNumbersResponseDTO.class);
    }

    // Retrieve single number
    public TelnyxMessagingPhoneNumberDTO get(String id) {
        return client().get()
                .uri("/v2/phone_numbers/messaging/{id}", id)
                .retrieve()
                .body(TelnyxMessagingPhoneNumberDTO.class);
    }

    // Update single number
    public TelnyxMessagingPhoneNumberDTO update(String id, TelnyxUpdatePhoneNumberRequestDTO body) {
        return client().patch()
                .uri("/v2/phone_numbers/messaging/{id}", id)
                .body(body)
                .retrieve()
                .body(TelnyxMessagingPhoneNumberDTO.class);
    }

    // Bulk update
    public TelnyxBulkUpdateResponseDTO bulkUpdate(TelnyxBulkUpdateRequestDTO body) {
        return client().post()
                .uri("/v2/messaging_numbers_bulk_updates")
                .body(body)
                .retrieve()
                .body(TelnyxBulkUpdateResponseDTO.class);
    }

    // Bulk update status
    public TelnyxBulkUpdateResponseDTO bulkStatus(String orderId) {
        return client().get()
                .uri("/v2/messaging_numbers_bulk_updates/{orderId}", orderId)
                .retrieve()
                .body(TelnyxBulkUpdateResponseDTO.class);
    }
}


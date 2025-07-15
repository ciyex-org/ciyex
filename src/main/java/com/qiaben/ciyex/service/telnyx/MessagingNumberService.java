
// src/main/java/com/qiaben/ciyex/service/telnyx/MessagingNumberService.java
package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class MessagingNumberService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .build();
    }

    // List numbers
    public ListPhoneNumbersResponseDTO list(int pageNumber, int pageSize) {
        String url = UriComponentsBuilder
                .fromPath("/v2/phone_numbers/messaging")
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .toUriString();

        return client().get()
                .uri(url)
                .retrieve()
                .body(ListPhoneNumbersResponseDTO.class);
    }

    // Retrieve single number
    public MessagingPhoneNumberDTO get(String id) {
        return client().get()
                .uri("/v2/phone_numbers/messaging/{id}", id)
                .retrieve()
                .body(MessagingPhoneNumberDTO.class);
    }

    // Update single number
    public MessagingPhoneNumberDTO update(String id, UpdatePhoneNumberRequestDTO body) {
        return client().patch()
                .uri("/v2/phone_numbers/messaging/{id}", id)
                .body(body)
                .retrieve()
                .body(MessagingPhoneNumberDTO.class);
    }

    // Bulk update
    public BulkUpdateResponseDTO bulkUpdate(BulkUpdateRequestDTO body) {
        return client().post()
                .uri("/v2/messaging_numbers_bulk_updates")
                .body(body)
                .retrieve()
                .body(BulkUpdateResponseDTO.class);
    }

    // Bulk update status
    public BulkUpdateResponseDTO bulkStatus(String orderId) {
        return client().get()
                .uri("/v2/messaging_numbers_bulk_updates/{orderId}", orderId)
                .retrieve()
                .body(BulkUpdateResponseDTO.class);
    }
}


package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ListPhoneMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RetrievePhoneNumbersService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public ListPhoneMessagingProfileDto listPhoneNumbers(String id, int pageNumber, int pageSize) {
        String url = String.format("%s/v2/messaging_profiles/%s/phone_numbers?page[number]=%d&page[size]=%d",
                telnyxProperties.getApiBaseUrl(), id, pageNumber, pageSize);

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(ListPhoneMessagingProfileDto.class);
    }
}

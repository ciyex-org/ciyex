package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ListShortcodesMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ListShortcodesMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public ListShortcodesMessagingProfileDto getShortcodes(String id, int pageNumber, int pageSize) {
        String url = String.format("%s/v2/messaging_profiles/%s/short_codes", telnyxProperties.getApiBaseUrl(), id);

        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .toUriString();

        return restClient
                .get()
                .uri(finalUrl)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(ListShortcodesMessagingProfileDto.class);
    }
}

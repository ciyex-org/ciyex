package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ConferenceListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ConferenceListService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public ConferenceListResponseDto listConferences(
            String name,
            String status,
            Integer page,
            Integer size) {

        StringBuilder uriBuilder = new StringBuilder(telnyxProperties.getApiBaseUrl() + "/v2/conferences?");

        if (name != null && !name.isEmpty()) {
            uriBuilder.append("filter[name]=").append(name).append("&");
        }
        if (status != null && !status.isEmpty()) {
            uriBuilder.append("filter[status]=").append(status).append("&");
        }
        if (page != null && page > 0) {
            uriBuilder.append("page[number]=").append(page).append("&");
        }
        if (size != null && size > 0) {
            uriBuilder.append("page[size]=").append(size).append("&");
        }

        // Trim trailing '&' or '?' if needed
        String uri = uriBuilder.toString().replaceAll("[&?]$", "");

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(ConferenceListResponseDto.class);
    }
}

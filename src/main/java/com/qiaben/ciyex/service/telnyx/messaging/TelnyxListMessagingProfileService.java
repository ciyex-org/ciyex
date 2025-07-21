package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxListMessagingProfileService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxListMessagingProfileDto listMessagingProfiles(
            Integer pageNumber,
            Integer pageSize,
            String filterName
    ) {
        String url = telnyxProperties.getApiBaseUrl() + "/messaging_profiles";

        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();

        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder = uriBuilder.path(url);
                    if (pageNumber != null) uriBuilder.queryParam("page[number]", pageNumber);
                    if (pageSize != null) uriBuilder.queryParam("page[size]", pageSize);
                    if (filterName != null && !filterName.isBlank())
                        uriBuilder.queryParam("filter[name]", filterName);
                    return uriBuilder.build();
                });

        return request.retrieve()
                .body(TelnyxListMessagingProfileDto.class);
    }
}

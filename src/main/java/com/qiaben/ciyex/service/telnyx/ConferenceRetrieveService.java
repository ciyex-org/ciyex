package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ConferenceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ConferenceRetrieveService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public ConferenceResponseDto getConferenceById(String id) {
        String uri = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + id;

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(ConferenceResponseDto.class);
    }

    // (Include listConferences here if you already added it)
}
